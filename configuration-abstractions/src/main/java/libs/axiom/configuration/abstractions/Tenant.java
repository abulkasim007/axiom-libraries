package libs.axiom.configuration.abstractions;

import java.util.UUID;

public record Tenant(
        UUID id,
        String name,
        String readDatabaseName,
        String stateDatabaseName,
        String eventDatabaseName,
        String storageBucketName,
        String cacheDatabaseName,
        String readServerConnectionString,
        String stateServerConnectionString,
        String eventServerConnectionString,
        String storageServerConnectionString,
        String cacheServerConnectionString,
        String serviceBusConnectionString
) {
}