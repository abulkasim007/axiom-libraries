package libs.axiom.messaging.abstractions;

import java.util.UUID;

public interface Message {
    UUID getCorrelationId();

    UserContext getUserContext();

    boolean isRedeliver();

    void setCorrelationId(UUID correlationId);

    void setUserContext(UserContext userContext);

    void setRedeliver(boolean redeliver);
}
