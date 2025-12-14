package libs.axiom.data.abstractions.reliability;

import jakarta.inject.Inject;
import libs.axiom.host.abstractions.HostedService;

public class DefaultOutboxProcessorHostedService implements HostedService {

    private final OutboxProcessor OutboxProcessor;

    @Inject
    public DefaultOutboxProcessorHostedService(OutboxProcessor OutboxProcessor) {
        this.OutboxProcessor = OutboxProcessor;
    }

    @Override
    public void start() {
        this.OutboxProcessor.start();
    }

    @Override
    public void stop() {
        this.OutboxProcessor.stop();
    }
}
