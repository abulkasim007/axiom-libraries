package libs.axiom.host.implementations.serializers;

import libs.axiom.serialization.abstractions.SerializationFormat;
import libs.axiom.serialization.abstractions.Serializer;
import libs.axiom.serialization.abstractions.SerializerProvider;

import java.util.HashMap;
import java.util.Map;

public class DefaultSerializerProvider implements SerializerProvider {

    private static final Map<SerializationFormat, Serializer> SERIALIZERS = new HashMap<>() {
        {
            put(SerializationFormat.JSON, new JsonSerializer());
        }
    };

    @Override
    public Serializer getSerializer(SerializationFormat serializationFormat) {
        return SERIALIZERS.get(serializationFormat);
    }
}
