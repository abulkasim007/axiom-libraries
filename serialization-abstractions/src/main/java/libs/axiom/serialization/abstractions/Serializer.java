package libs.axiom.serialization.abstractions;

import java.util.Map;

public interface Serializer {

    String serialize(Object object);

    byte[] serializeToBytes(Object object);

    <T> T deserialize(String json, Class<T> type);

    <T> T deserialize(byte[] json, Class<T> type);

    <T> T deserialize(Map<String, Object> jsonMap, Class<T> type);
}
