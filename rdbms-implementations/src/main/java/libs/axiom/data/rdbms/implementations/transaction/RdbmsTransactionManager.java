package libs.axiom.data.rdbms.implementations.transaction;

import jakarta.persistence.EntityManager;
import libs.axiom.messaging.abstractions.transaction.TransactionManager;

public class RdbmsTransactionManager implements TransactionManager {

    private static final String MANAGER_NAME = RdbmsTransactionManager.class.getName();

    private final EntityManager entityManager;

    public RdbmsTransactionManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void close() {
        entityManager.close();
    }

    @Override
    public String name() {
        return MANAGER_NAME;
    }

    @Override
    public void commit() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public void rollback() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }
}