package libs.axiom.messaging.abstractions;

import java.util.Map;

public class NoOpBus implements Bus {

    private static final String NOOP_BUS_MESSAGE = """
            [NoOpBus] This is a placeholder bus implementation.
            No message was actually sent or published.
            To enable messaging, include a real Bus implementation (e.g., RabbitMQBus).
            """;

    @Override
    public <T extends Command> void send(T command) {
        throw new RuntimeException(NOOP_BUS_MESSAGE);
    }

    @Override
    public <T extends Command> void send(T command, UserContext userContext) {
        throw new RuntimeException(NOOP_BUS_MESSAGE);
    }

    @Override
    public <T extends Event> void publish(T eventObject) {
        throw new RuntimeException(NOOP_BUS_MESSAGE);
    }

    @Override
    public <T extends Event> void publish(T eventObject, UserContext userContext) {
        throw new RuntimeException(NOOP_BUS_MESSAGE);
    }

    @Override
    public void publish(boolean exchange, String topic, Map<String, Object> headers, byte[] body) {
        throw new RuntimeException(NOOP_BUS_MESSAGE);
    }
}
