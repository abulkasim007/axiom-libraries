package libs.axiom.data.rdbms.implementations.dbcontext;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.data.abstractions.DataSourceType;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.RepositoryCache;
import libs.axiom.data.rdbms.implementations.configurations.EntityMapProvider;
import libs.axiom.data.rdbms.implementations.repositories.RdbmsCqrsRepository;
import libs.axiom.messaging.abstractions.ThreadLocalContextProvider;
import libs.axiom.messaging.abstractions.UserContext;
import libs.axiom.messaging.abstractions.UserContextProvider;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;

import java.util.*;

import static libs.axiom.data.abstractions.utils.DatabaseUtils.getDatabaseId;

public class RdbmsRepositoryCache implements RepositoryCache {

    private static final String SETTINGS_SPLITTER = ";";
    private static final String KEY_VALUE_SPLITTER = "=";

    private final String serviceId;
    private final UserContextProvider userContextProvider;
    private final Map<String, RdbmsCqrsRepository> cachedSqlDatabases = new HashMap<>();
    private final Map<String, EntityManagerFactory> cachedEntityManagerFactories = new HashMap<>();


    @Inject
    public RdbmsRepositoryCache(EntityMapProvider entityMapProvider,
                                UserContextProvider userContextProvider,
                                ThreadLocalContextProvider threadLocalContextProvider,
                                ServiceConfigurationProvider serviceConfigurationProvider) {

        this.userContextProvider = userContextProvider;

        Map<DatabaseType, List<Class<?>>> entityTypes = entityMapProvider.get();

        Service service = serviceConfigurationProvider.getCurrentService();

        this.serviceId = service.id();

        for (Vertical vertical : service.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                // Read database
                if (isValidConnectionString(tenant.readServerConnectionString())) {

                    List<Class<?>> readEntities = entityTypes.getOrDefault(DatabaseType.READ, Collections.emptyList());

                    if (!readEntities.isEmpty()) {
                        SessionFactory readSessionFactory =
                                getSqlDatabase(tenant.readServerConnectionString(), readEntities);

                        String readDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.READ);
                        cachedSqlDatabases.put(readDatabaseId, new RdbmsCqrsRepository(readSessionFactory, threadLocalContextProvider));
                        cachedEntityManagerFactories.put(readDatabaseId, readSessionFactory);
                    }
                }

                // State database
                if (isValidConnectionString(tenant.stateServerConnectionString())) {

                    List<Class<?>> stateEntities = entityTypes.getOrDefault(DatabaseType.STATE, Collections.emptyList());

                    if (!stateEntities.isEmpty()) {
                        SessionFactory stateSessionFactory =
                                getSqlDatabase(tenant.stateServerConnectionString(), stateEntities);

                        String stateDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.STATE);
                        cachedSqlDatabases.put(stateDatabaseId, new RdbmsCqrsRepository(stateSessionFactory, threadLocalContextProvider));
                        cachedEntityManagerFactories.put(stateDatabaseId, stateSessionFactory);
                    }
                }

                // Event database
                if (isValidConnectionString(tenant.eventServerConnectionString())) {

                    List<Class<?>> eventEntities = entityTypes.getOrDefault(DatabaseType.EVENT, Collections.emptyList());

                    if (!eventEntities.isEmpty()) {
                        SessionFactory eventSessionFactory =
                                getSqlDatabase(tenant.eventServerConnectionString(), eventEntities);

                        String eventDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.EVENT);
                        cachedSqlDatabases.put(eventDatabaseId,
                                new RdbmsCqrsRepository(eventSessionFactory, threadLocalContextProvider));
                        cachedEntityManagerFactories.put(eventDatabaseId, eventSessionFactory);
                    }
                }
            }
        }
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.RDBMS;
    }

    public Repository getTenantRepository(DatabaseType databaseType) {
        return getTenantRepository(databaseType, userContextProvider.getUserContext());
    }

    public Repository getTenantRepository(DatabaseType databaseType, UserContext userContext) {
        String databaseId = getDatabaseId(this.serviceId, userContext.getTenantId(), userContext.getVerticalId(), databaseType);

        return cachedSqlDatabases.get(databaseId);
    }

    public Repository getTenantRepository(DatabaseType databaseType, UUID tenantId, UUID verticalId) {
        String databaseId = getDatabaseId(this.serviceId, tenantId, verticalId, databaseType);

        Repository repository = cachedSqlDatabases.get(databaseId);

        if (repository == null) {
            throw new IllegalStateException("There is no repository configured for data source type: RDBMS, database type: " + databaseType + " or tenant id: " + tenantId+ ". Please check your connection strings.");
        }

        return repository;
    }

    public EntityManagerFactory getEntityManagerFactory(DatabaseType databaseType, UUID tenantId, UUID verticalId) {
        String databaseId = getDatabaseId(this.serviceId, tenantId, verticalId, databaseType);

        EntityManagerFactory entityManagerFactory = cachedEntityManagerFactories.get(databaseId);

        if (entityManagerFactory == null) {
            throw new IllegalStateException("There is no EntityManagerFactory configured for data source type: RDBMS, database type: " + databaseType + " or tenant id: " + tenantId+ ". Please check your connection strings.");
        }

        return entityManagerFactory;
    }

    private static SessionFactory getSqlDatabase(String databaseConnectionString, List<Class<?>> annotatedEntityClasses) {

        Properties hibernateProperties = createHibernateProperties(databaseConnectionString);

        // Create ServiceRegistry
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(hibernateProperties)
                .build();

        // Create MetadataSources
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);

        for (Class<?> annotatedEntityClass : annotatedEntityClasses) {
            metadataSources.addAnnotatedClass(annotatedEntityClass);
        }

        // Create Metadata
        Metadata metadata = metadataSources.getMetadataBuilder().build();

        // Build SessionFactory
        return metadata.getSessionFactoryBuilder().build();
    }

    private static boolean isValidConnectionString(String connectionString) {
        return connectionString != null && connectionString.contains("jakarta.persistence.jdbc.url");
    }

    public static Properties createHibernateProperties(String connectionString) {

        Properties hibernateProperties = new Properties();

        // hibernate.connection.autocommit=false;
        hibernateProperties.put(AvailableSettings.AUTOCOMMIT, "false");

        // hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
        hibernateProperties.put(AvailableSettings.CONNECTION_PROVIDER,
                "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

        // hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
        hibernateProperties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        // hibernate.current_session_context_class=thread;
        hibernateProperties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");

        // hibernate.allow_update_outside_transaction=false;
        hibernateProperties.put(AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, "false");

        // hibernate.connection.provider_disables_autocommit=true;
        hibernateProperties.put(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, "true");

        String[] keyValuePairs = connectionString.split(SETTINGS_SPLITTER);

        for (String pair : keyValuePairs) {
            String[] entry = pair.split(KEY_VALUE_SPLITTER, 2);
            if (entry.length != 2) {
                throw new IllegalArgumentException(
                        "Provided connection string [ " + connectionString + " ] is invalid. Sample valid connection string: [ " +
                                validConnectionString + " ]");
            }
            hibernateProperties.put(entry[0], entry[1]);
        }
        return hibernateProperties;
    }

    private static final String validConnectionString =

            // Hibernate Settings
            "jakarta.persistence.jdbc.driver=org.postgresql.Driver;" +
                    "jakarta.persistence.jdbc.url=jdbc:postgresql://localhost/demo_database;" +
                    "jakarta.persistence.jdbc.user=postgres;" +
                    "jakarta.persistence.jdbc.password=12345;" +
                    "hibernate.hbm2ddl.auto=update;" +
                    "hibernate.show_sql=true;" +
                    "hibernate.format_sql=true;" +
                    "hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;" +

                    // Hikari Connection Pool Settings
                    "hibernate.connection.autocommit=false;" +
                    "hibernate.hikari.minimumIdle=5;" +
                    "hibernate.hikari.maximumPoolSize=10;" +
                    "hibernate.hikari.idleTimeout=30000;" +
                    "hibernate.hikari.maxLifetime=1800000;" +
                    "hibernate.hikari.connectionTimeout=1800000;" +
                    "hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider;" +
                    "hibernate.connection.provider_disables_autocommit=true;" +
                    "hibernate.allow_update_outside_transaction=true";
}


