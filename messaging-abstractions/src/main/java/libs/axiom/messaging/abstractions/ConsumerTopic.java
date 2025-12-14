package libs.axiom.messaging.abstractions;

import libs.axiom.serialization.abstractions.SerializationFormat;

public class ConsumerTopic<T extends Message> extends Topic {

    private final int prefetchSize;
    private final Class<T> messageClass;

    public ConsumerTopic(String name, String type, int prefetchSize, SerializationFormat serializationFormat, Class<T> messageClass) {
        super(name, type, serializationFormat);
        this.prefetchSize = prefetchSize;
        this.messageClass = messageClass;
    }

    public int getPrefetchSize() {
        return prefetchSize;
    }

    public Class<T> getMessageClass() {
        return messageClass;
    }
}

