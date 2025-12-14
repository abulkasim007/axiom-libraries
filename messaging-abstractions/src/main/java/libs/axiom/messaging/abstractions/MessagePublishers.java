package libs.axiom.messaging.abstractions;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import libs.axiom.serialization.abstractions.SerializationFormat;

public class MessagePublishers {
    private final Multibinder<ProducerTopic> multibinder;

    private MessagePublishers(Binder binder) {
        this.multibinder = Multibinder.newSetBinder(binder, ProducerTopic.class);
    }

    public static MessagePublishers from(Binder binder) {
        return new MessagePublishers(binder);
    }

    public <T extends Message> MessagePublishers add(Class<T> messageType, SerializationFormat serializationFormat) {
        multibinder.addBinding().toInstance(new ProducerTopic(messageType.getTypeName(), Topic.FANOUT, serializationFormat));
        return this;
    }
}
