package libs.axiom.messaging.rabbitmq.implementations.producer;

import com.rabbitmq.client.Channel;
import libs.axiom.serialization.abstractions.SerializationFormat;

import java.util.UUID;

public record ProducerInfo(String name,
                           String type,
                           Channel channel,
                           UUID tenantId,
                           boolean isDeadLetter,
                           SerializationFormat serializationFormat) {

    public String key() {
        return calculateKey(name, tenantId);
    }

    public static String calculateKey(String name, UUID tenantId) {
        return name + "-" + tenantId;
    }
}
