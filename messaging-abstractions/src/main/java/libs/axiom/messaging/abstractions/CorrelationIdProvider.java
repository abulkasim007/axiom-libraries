package libs.axiom.messaging.abstractions;

import java.util.UUID;

public interface CorrelationIdProvider {
    UUID getCorrelationId();
}
