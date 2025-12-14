package libs.axiom.messaging.abstractions;

import libs.axiom.serialization.abstractions.SerializationFormat;
import libs.axiom.serialization.abstractions.SerializerProvider;

public interface MessageDeserializer<T extends Message> {
    T deserialize(byte[] message, SerializerProvider serializerProvider, SerializationFormat serializationFormat);
}