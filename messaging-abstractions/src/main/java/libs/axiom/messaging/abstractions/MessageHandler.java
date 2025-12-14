package libs.axiom.messaging.abstractions;

public interface MessageHandler<T extends Message> {
    void handle(T message);
}
