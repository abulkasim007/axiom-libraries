package libs.axiom.data.abstractions.reliability;


import libs.axiom.messaging.abstractions.transaction.Outbox;

public interface OutboxProcessor {
    void submit(Outbox item);
    void start();
    void stop();
}