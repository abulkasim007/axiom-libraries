package libs.axiom.data.rdbms.implementations.repositories;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.rdbms.implementations.dbcontext.RdbmsRepositoryCache;

import java.util.UUID;

public class OutboxRepositoryFactory {

    private final RdbmsRepositoryCache rdbmsRepositoryCache;

    @Inject
    public OutboxRepositoryFactory(RdbmsRepositoryCache rdbmsRepositoryCache) {
        this.rdbmsRepositoryCache = rdbmsRepositoryCache;
    }

    public OutboxRepository getOutboxRepository(UUID tenantId, UUID verticalId) {

        EntityManagerFactory eventEntityManagerFactory = rdbmsRepositoryCache.getEntityManagerFactory(DatabaseType.EVENT, tenantId, verticalId);
        EntityManagerFactory stateEntityManagerFactory = rdbmsRepositoryCache.getEntityManagerFactory(DatabaseType.STATE, tenantId, verticalId);

        return new OutboxRepository(stateEntityManagerFactory.createEntityManager(), eventEntityManagerFactory.createEntityManager());
    }

}
