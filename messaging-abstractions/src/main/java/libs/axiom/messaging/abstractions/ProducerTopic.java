package libs.axiom.messaging.abstractions;

import libs.axiom.serialization.abstractions.SerializationFormat;

public class ProducerTopic extends Topic {

    public ProducerTopic(String name, String type, SerializationFormat serializationFormat) {
        super(name, type, serializationFormat);
    }
}