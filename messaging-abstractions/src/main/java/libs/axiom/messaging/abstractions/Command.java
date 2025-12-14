package libs.axiom.messaging.abstractions;

import java.util.UUID;

public class Command implements Message {

    private boolean redeliver;

    private UUID correlationId;

    private UserContext userContext;

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

    @Override
    public boolean isRedeliver() {
        return redeliver;
    }

    @Override
    public void setRedeliver(boolean redeliver) {
        this.redeliver = redeliver;
    }
}
