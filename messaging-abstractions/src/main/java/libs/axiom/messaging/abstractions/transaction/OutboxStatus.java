package libs.axiom.messaging.abstractions.transaction;

public enum OutboxStatus {
    PENDING,
    EVENTS_SAVED,
    EVENTS_PUBLISHED,
    CLEARED
}
