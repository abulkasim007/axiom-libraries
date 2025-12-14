package libs.axiom.host.implementations.extensions;

import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.CorrelationIdProvider;
import libs.axiom.messaging.abstractions.MessageProvider;

import java.util.UUID;

public class DefaultCorrelationIdProvider implements CorrelationIdProvider {

    private final MessageProvider messageProvider;

    @Inject
    public DefaultCorrelationIdProvider(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    public UUID getCorrelationId() {
        return messageProvider.getMessage().getCorrelationId();
    }
}
