package libs.axiom.data.rdbms.implementations.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import libs.axiom.data.abstractions.reliability.OutboxImpl;
import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.transaction.OutboxStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OutboxRepository {

    private final EntityManager stateEntityManager;
    private final EntityManager eventEntityManager;

    public OutboxRepository(EntityManager stateEntityManager, EntityManager eventEntityManager) {
        this.stateEntityManager = stateEntityManager;
        this.eventEntityManager = eventEntityManager;
    }

    public List<OutboxImpl> getOutboxes(OutboxStatus outboxStatus, int pageSize) {
        ensureTransactionActive(stateEntityManager);

        Query query = stateEntityManager.createQuery(
                        "SELECT o FROM OutboxImpl o WHERE o.outboxStatus = :outboxStatus", OutboxImpl.class)
                .setParameter("outboxStatus", outboxStatus)
                .setMaxResults(pageSize);

        return query.getResultList();
    }

    public List<UUID> getSavedEventIds(List<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        ensureTransactionActive(stateEntityManager);

        Query query = stateEntityManager.createQuery(
                "SELECT e.id FROM Event e WHERE e.id IN :eventIds");
        query.setParameter("eventIds", eventIds);

        @SuppressWarnings("unchecked")
        List<UUID> result = query.getResultList();
        return result;
    }

    public void saveEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        ensureTransactionActive(eventEntityManager);

        for (Event event : events) {
            eventEntityManager.persist(event);
        }
    }

    public void saveChanges() {
        if (stateEntityManager.getTransaction().isActive()) {
            stateEntityManager.getTransaction().commit();
        }

        if (eventEntityManager.getTransaction().isActive()) {
            eventEntityManager.getTransaction().commit();
        }

        if (stateEntityManager.isOpen()) {
            stateEntityManager.close();
        }
        if (eventEntityManager.isOpen()) {
            eventEntityManager.close();
        }
    }

    public OutboxImpl getOutbox(UUID id) {
        if (id == null) {
            return null;
        }

        ensureTransactionActive(stateEntityManager);

        try {
            Query query = stateEntityManager.createQuery(
                    "SELECT o FROM OutboxImpl o WHERE o.id = :id", OutboxImpl.class);
            query.setParameter("id", id);
            return (OutboxImpl) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private void ensureTransactionActive(EntityManager entityManager) {
        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
    }
}
