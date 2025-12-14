package libs.axiom.messaging.abstractions;

import jakarta.persistence.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Event implements Message {

    @Id
    private UUID id;
    private boolean redeliver;
    private UUID correlationId;
    @OneToOne(cascade = CascadeType.PERSIST)
    private UserContext userContext;
    private UUID aggregateRootId;
    private int aggregateRootVersion;
    private Date timeStamp;
    private String source;
    private String name;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getAggregateRootId() {
        return aggregateRootId;
    }

    public void setAggregateRootId(UUID aggregateRootId) {
        this.aggregateRootId = aggregateRootId;
    }

    public int getAggregateRootVersion() {
        return aggregateRootVersion;
    }

    public void setAggregateRootVersion(int aggregateRootVersion) {
        this.aggregateRootVersion = aggregateRootVersion;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isRedeliver() {
        return redeliver;
    }

    @Override
    public void setRedeliver(boolean redeliver) {
        this.redeliver = redeliver;
    }
}

