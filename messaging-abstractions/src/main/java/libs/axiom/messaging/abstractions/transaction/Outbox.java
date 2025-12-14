package libs.axiom.messaging.abstractions.transaction;

import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.UserContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface Outbox {

    UUID getId();
    void setId(UUID id);

    UUID getEntityId();
    void setEntityId(UUID entityId);

    int getEntityVersion();
    void setEntityVersion(int entityVersion);

    String getEntityName();
    void setEntityName(String entityName);

    List<Event> getEvents();
    void setEvents(List<Event> events);

    UUID getCorrelationId();
    void setCorrelationId(UUID correlationId);

    UserContext getUserContext();
    void setUserContext(UserContext userContext);

    Instant getTimeStamp();
    void setTimeStamp(Instant timeStamp);

    int getAttempt();
    void setAttempt(int attempt);
    void updateAttempts();

    OutboxStatus getOutboxStatus();
    void setOutboxStatus(OutboxStatus status);

    boolean isLoaded();
    void setLoaded(boolean loaded);

    boolean isPersistent();
    void setPersistent(boolean persistent);
}
