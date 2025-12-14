package libs.axiom.data.abstractions.models;

import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.transaction.Outbox;

import java.util.ArrayList;
import java.util.List;

public class AggregateRoot extends Entity {

    private transient final List<Event> events = new ArrayList<>();

    private transient Outbox outbox;

    protected <T extends Event> void addEvent(T eventObject) {
        this.events.add(eventObject);
    }

    public List<Event> getEvents() {
        return events;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }
}
