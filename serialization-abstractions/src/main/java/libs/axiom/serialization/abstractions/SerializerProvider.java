package libs.axiom.serialization.abstractions;

public interface SerializerProvider {

    Serializer getSerializer(SerializationFormat serializationFormat);
}