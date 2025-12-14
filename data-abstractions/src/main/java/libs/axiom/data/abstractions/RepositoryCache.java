package libs.axiom.data.abstractions;

import java.util.UUID;

public interface RepositoryCache {

    DataSourceType getDataSourceType();

    Repository getTenantRepository(DatabaseType databaseType);
    Repository getTenantRepository(DatabaseType databaseType, UUID tenantId, UUID verticalId);
}