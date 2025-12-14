package libs.axiom.data.mongodb.implementations.dbcontext;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.data.abstractions.DataSourceType;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.RepositoryCache;
import libs.axiom.data.mongodb.implementations.repositories.MongoCqrsRepository;
import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.UserContext;
import libs.axiom.messaging.abstractions.UserContextProvider;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;

import static libs.axiom.data.abstractions.utils.DatabaseUtils.getDatabaseId;

public class MongoRepositoryCache implements RepositoryCache {

    private final String serviceId;
    private final UserContextProvider userContextProvider;
    private final Map<String, MongoCqrsRepository> cachedMongoCqrsRepositories;
    private final Map<ConnectionString, MongoClient> mongoClientPool = new HashMap<>();

    @Inject
    public MongoRepositoryCache(
            UserContextProvider userContextProvider,
            ServiceConfigurationProvider serviceRegistryProvider) {
        this.userContextProvider = userContextProvider;
        Service currentService = serviceRegistryProvider.getCurrentService();

        this.serviceId = currentService.id();

        CodecRegistry codecRegistry = createCodecRegistry(currentService.eventPackages());
        this.cachedMongoCqrsRepositories = buildRepositoryCache(currentService, codecRegistry);
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.MONGO;
    }

    @Override
    public Repository getTenantRepository(DatabaseType databaseType) {

        UserContext userContext = userContextProvider.getUserContext();

        return getTenantRepository(databaseType, userContext.getTenantId(), userContext.getVerticalId());
    }

    @Override
    public Repository getTenantRepository(DatabaseType databaseType, UUID tenantId, UUID verticalId) {

        String repositoryId = getDatabaseId(this.serviceId, tenantId, verticalId, databaseType);

        Repository repository = cachedMongoCqrsRepositories.get(repositoryId);

        if (repository == null) {
            throw new IllegalStateException("There is no repository configured for data source type: MONGO, database type: " + databaseType + " or tenant id: " + tenantId + ". Please check your connection strings.");
        }

        return repository;
    }


    private Map<String, MongoCqrsRepository> buildRepositoryCache(Service service, CodecRegistry codecRegistry) {
        List<Tenant> allTenants = Arrays.stream(service.verticals())
                .flatMap(vertical -> Arrays.stream(vertical.tenants()))
                .toList();

        Map<String, MongoCqrsRepository> repositories = new HashMap<>();

        for (Tenant tenant : allTenants) {
            // Read DB
            if (hasDbConfig(tenant.readServerConnectionString(), tenant.readDatabaseName())) {
                String repositoryId = getDatabaseId(this.serviceId, tenant.id(), tenant.id(), DatabaseType.READ);
                MongoCqrsRepository ctx = createMongoDbContext(repositoryId, tenant.readDatabaseName(), tenant.readServerConnectionString(), codecRegistry);
                repositories.put(repositoryId, ctx);
            }

            // State DB
            if (hasDbConfig(tenant.stateServerConnectionString(), tenant.stateDatabaseName())) {

                String repositoryId = getDatabaseId(this.serviceId, tenant.id(), tenant.id(), DatabaseType.STATE);
                MongoCqrsRepository ctx = createMongoDbContext(repositoryId, tenant.stateDatabaseName(), tenant.stateServerConnectionString(), codecRegistry);
                repositories.put(repositoryId, ctx);
            }

            // Event DB
            if (hasDbConfig(tenant.eventServerConnectionString(), tenant.eventDatabaseName())) {
                String repositoryId = getDatabaseId(this.serviceId, tenant.id(), tenant.id(), DatabaseType.EVENT);
                MongoCqrsRepository ctx = createMongoDbContext(repositoryId, tenant.eventDatabaseName(), tenant.eventServerConnectionString(), codecRegistry);
                repositories.put(repositoryId, ctx);
            }
        }

        return repositories;
    }

    private boolean hasDbConfig(String connectionString, String dbName) {
        return (connectionString != null && !connectionString.trim().isEmpty()) ||
                (dbName != null && !dbName.trim().isEmpty());
    }

    private MongoCqrsRepository createMongoDbContext(String contextId, String databaseName, String connectionString, CodecRegistry codecRegistry) {
        if (connectionString == null || connectionString.isEmpty()) {
            throw new IllegalArgumentException("Connection string is required for context: " + contextId);
        }

        ConnectionString mongoUrl = new ConnectionString(connectionString);

        MongoClient client = mongoClientPool.computeIfAbsent(mongoUrl, url -> {
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyConnectionString(url)
                    .codecRegistry(codecRegistry)
                    .applyToConnectionPoolSettings(builder -> builder.maxConnecting(5));

            return MongoClients.create(settingsBuilder.build());
        });


        return new MongoCqrsRepository(databaseName, client);
    }

    private static CodecRegistry createCodecRegistry(String[] eventPackages) {
        PojoCodecProvider.Builder providerBuilder = PojoCodecProvider.builder()
                .automatic(true);

        ClassModel<?> eventClassModel = ClassModel.builder(Event.class)
                .discriminator(Event.class.getName())
                .discriminatorKey("_t")
                .enableDiscriminator(true)
                .build();
        providerBuilder.register(eventClassModel);

        Set<Class<? extends Event>> eventTypes = findConcreteEventSubtypes(eventPackages);
        for (Class<? extends Event> eventType : eventTypes) {
            ClassModel<?> model = ClassModel.builder(eventType)
                    .discriminator(eventType.getName())
                    .enableDiscriminator(true)
                    .build();
            providerBuilder.register(model);
        }

        CodecRegistry pojoRegistry = CodecRegistries.fromProviders(providerBuilder.build());
        return CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                pojoRegistry
        );
    }

    private static Set<Class<? extends Event>> findConcreteEventSubtypes(String[] eventPackages) {
        Set<Class<? extends Event>> subtypes = new HashSet<>();

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .ignoreParentClassLoaders()
                .acceptPackages(eventPackages)
                .scan()) {

            ClassInfoList eventSubclasses = scanResult.getSubclasses(Event.class.getName());

            for (ClassInfo classInfo : eventSubclasses) {
                if (classInfo.isAbstract() || classInfo.isInterface()) {
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> clazz = (Class<? extends Event>) Class.forName(classInfo.getName());
                    subtypes.add(clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class: " + classInfo.getName(), e);
                }
            }
        }

        return subtypes;
    }
}