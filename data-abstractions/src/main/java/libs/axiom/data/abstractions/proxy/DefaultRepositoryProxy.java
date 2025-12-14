package libs.axiom.data.abstractions.proxy;

import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.data.abstractions.*;
import libs.axiom.messaging.abstractions.UserContext;
import libs.axiom.messaging.abstractions.UserContextProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static libs.axiom.data.abstractions.utils.DatabaseUtils.getDatabaseId;

public class DefaultRepositoryProxy implements RepositoryProxy {

    private final String serviceId;
    private final UserContextProvider userContextProvider;
    private final Map<String, DataSourceType> tenantDataSourceTypeMap;
    private final Map<DataSourceType, RepositoryCache> dataSourceTypeRepositoryMap;

    @Inject
    public DefaultRepositoryProxy(
            RepositoryCache repositoryCaches,
            UserContextProvider userContextProvider,
            ServiceConfigurationProvider serviceConfigurationProvider) {
        this.userContextProvider = userContextProvider;

        Service service = serviceConfigurationProvider.getCurrentService();

        this.serviceId = service.id();

        this.tenantDataSourceTypeMap = this.buildTenantDataSourceMap(service);

        this.dataSourceTypeRepositoryMap = Map.of(repositoryCaches.getDataSourceType(), repositoryCaches);

        this.validateCache();
    }

    @Override
    public Repository get(DatabaseType databaseType) {
        UserContext userContext = this.userContextProvider.getUserContext();
        String tenantId = getDatabaseId(serviceId, userContext.getTenantId(), userContext.getVerticalId(), databaseType);
        DataSourceType dataSourceType = this.tenantDataSourceTypeMap.get(tenantId);
        RepositoryCache repositoryCache = dataSourceTypeRepositoryMap.get(dataSourceType);

        if (repositoryCache == null) {

            String errorMessage = "Data source type: " + databaseType + " for tenant id: " + tenantId +
                    " is not connected. Please add a MongoDbModule, JpaModule, or another appropriate data source module to dependency according to the service configuration file.";

            throw new RuntimeException(errorMessage);
        }

        return repositoryCache.getTenantRepository(databaseType);
    }

    private void validateCache() {

        if (this.dataSourceTypeRepositoryMap.isEmpty() ||
                this.dataSourceTypeRepositoryMap.values().stream().allMatch(Objects::isNull)) {
            throw new IllegalStateException(
                    "You have added the repository proxy module, but no data source is connected. Please add a MongoDbModule, JpaModule, or another appropriate data source module to dependency.");
        }

        if (this.tenantDataSourceTypeMap.isEmpty()) {
            throw new IllegalStateException("No tenant data source type is mapped.");
        }

        for (var entry : this.tenantDataSourceTypeMap.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalStateException(
                        "Tenant id: " + entry.getKey() +
                                " is not mapped with any data source. Please check your service configuration file.");
            }
        }
    }


    private Map<String, DataSourceType> buildTenantDataSourceMap(Service service) {

        Map<String, DataSourceType> tenantDataSourceMap = new HashMap<>();

        for (Vertical vertical : service.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                // Read database
                if (isValidConnectionString(tenant.readServerConnectionString()) ||
                        isValidDatabaseName(tenant.readDatabaseName())) {

                    DataSourceType dataSourceType = determineDataSourceType(tenant.readServerConnectionString());

                    String readDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.READ);
                    tenantDataSourceMap.put(readDatabaseId, dataSourceType);
                }

                // State database
                if (isValidConnectionString(tenant.stateServerConnectionString()) ||
                        isValidDatabaseName(tenant.stateDatabaseName())) {
                    DataSourceType dataSourceType = determineDataSourceType(tenant.stateServerConnectionString());

                    String stateDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.STATE);
                    tenantDataSourceMap.put(stateDatabaseId, dataSourceType);
                }

                // Event database
                if (isValidConnectionString(tenant.eventServerConnectionString()) ||
                        isValidDatabaseName(tenant.eventDatabaseName())) {
                    DataSourceType dataSourceType = determineDataSourceType(tenant.eventServerConnectionString());

                    String eventDatabaseId = getDatabaseId(serviceId, tenant.id(), vertical.id(), DatabaseType.EVENT);
                    tenantDataSourceMap.put(eventDatabaseId, dataSourceType);
                }
            }
        }

        return Collections.unmodifiableMap(tenantDataSourceMap);
    }

    private static DataSourceType determineDataSourceType(String connectionString) {
        if (connectionString.contains("jakarta.persistence")) {
            return DataSourceType.RDBMS;
        }

        if (connectionString.contains("mongodb")) {
            return DataSourceType.MONGO;
        }

        throw new UnsupportedOperationException("Not supported at this moment. Connection string: " + connectionString);
    }

    private static boolean isValidDatabaseName(String databaseName) {
        return isNotNullOrEmpty(databaseName);
    }

    private static boolean isValidConnectionString(String connectionString) {
        return isNotNullOrEmpty(connectionString);
    }

    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}


