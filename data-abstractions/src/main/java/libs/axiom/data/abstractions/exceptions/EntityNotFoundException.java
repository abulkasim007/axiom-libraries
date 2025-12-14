package libs.axiom.data.abstractions.exceptions;

import java.util.UUID;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(UUID correlationId, UUID entityId, String messageType, String source) {
        super(buildErrorMessage(correlationId, entityId, messageType, source));
    }

    private static String buildErrorMessage(UUID correlationId, UUID entityId, String messageType, String source) {
        return String.format(
                "Entity Not Found | CorrelationId: %s | EntityId: %s | Source: %s | MessageType: %s",
                correlationId,
                entityId,
                source,
                messageType
        );
    }
}
