package libs.axiom.messaging.abstractions;

import java.util.Map;

public interface Bus {

    <T extends Command> void send(T command);

    <T extends Command> void send(T command, UserContext userContext);

    <T extends Event> void publish(T eventObject);

    <T extends Event> void publish(T eventObject, UserContext userContext);

    void publish(boolean exchange, String topic, Map<String, Object> headers, byte[] body);
}
