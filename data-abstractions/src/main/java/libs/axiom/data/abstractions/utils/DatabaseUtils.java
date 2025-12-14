package libs.axiom.data.abstractions.utils;

import libs.axiom.data.abstractions.DatabaseType;

import java.util.UUID;

public final class DatabaseUtils {
    public static String getDatabaseId(String serviceId, UUID tenantId, UUID verticalId, DatabaseType databaseType) {
        return serviceId + tenantId + verticalId + databaseType;
    }
}
