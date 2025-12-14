package libs.axiom.data.abstractions.reliability;

import libs.axiom.messaging.abstractions.transaction.Outbox;
import libs.axiom.messaging.abstractions.transaction.OutboxManager;

public class DefaultOutboxManager implements OutboxManager {

    private final Outbox outbox;

    private final OutboxProcessor outboxProcessor;

    public DefaultOutboxManager(Outbox outbox, OutboxProcessor outboxProcessor) {
        this.outbox = outbox;
        this.outboxProcessor = outboxProcessor;
    }

    @Override
    public void submit() {
        this.outboxProcessor.submit(outbox);
    }
}
