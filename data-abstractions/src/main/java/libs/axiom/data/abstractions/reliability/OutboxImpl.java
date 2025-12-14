package libs.axiom.data.abstractions.reliability;

import com.querydsl.core.annotations.QueryEntity;
import jakarta.persistence.*;
import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.UserContext;
import libs.axiom.messaging.abstractions.transaction.Outbox;
import libs.axiom.messaging.abstractions.transaction.OutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@QueryEntity
public class OutboxImpl implements Outbox {

    @Id
    private UUID id;
    private int attempt;
    private UUID entityId;
    private Instant timeStamp;
    private int entityVersion;
    private String entityName;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Event> events;
    private UUID correlationId;
    @OneToOne(cascade = CascadeType.ALL)
    private UserContext userContext;
    private OutboxStatus outboxStatus;

    private transient boolean loaded;
    private transient boolean persistent;

    public static Outbox from(Class<?> entityType, UUID entityId, int entityVersion, List<Event> events, UserContext userContext, UUID correlationId, boolean transactionalOutboxRequired) {
        Outbox outbox = new OutboxImpl();
        outbox.setId(UUID.randomUUID());
        outbox.setEntityId(entityId);
        outbox.setUserContext(userContext);
        outbox.setEvents(events);
        outbox.setEntityVersion(entityVersion);
        outbox.setEntityName(entityType.getName());
        outbox.setCorrelationId(correlationId);
        outbox.setTimeStamp(Instant.now());
        outbox.setPersistent(transactionalOutboxRequired);
        outbox.setOutboxStatus(OutboxStatus.PENDING);
        return outbox;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public int getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(int entityVersion) {
        this.entityVersion = entityVersion;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public OutboxStatus getOutboxStatus() {
        return outboxStatus;
    }

    public void setOutboxStatus(OutboxStatus outboxStatus) {
        this.outboxStatus = outboxStatus;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Instant timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public void updateAttempts(){
        this.attempt++;
    }
}

