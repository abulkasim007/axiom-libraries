package libs.axiom.host.implementations.serializers;

import com.alibaba.fastjson2.JSON;
import libs.axiom.serialization.abstractions.Serializer;

import java.util.Map;

public class JsonSerializer implements Serializer {

    @Override
    public String serialize(Object object) {
        return JSON.toJSONString(object);
    }

    @Override
    public byte[] serializeToBytes(Object object) {
        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> T deserialize(byte[] json, Class<T> type) {
        return JSON.parseObject(json, type);
    }

    @Override
    public <T> T deserialize(Map<String, Object> map, Class<T> type) {
        // Todo: Bad!!! never meant to be called in hot code path.
        String json = JSON.toJSONString(map);
        return JSON.parseObject(json, type);
    }
}
