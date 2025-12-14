package libs.axiom.messaging.rabbitmq.implementations.producer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.messaging.abstractions.*;
import libs.axiom.messaging.rabbitmq.implementations.connection.RabbitMQConnectionManager;
import libs.axiom.serialization.abstractions.SerializationFormat;
import libs.axiom.serialization.abstractions.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RabbitMQBus implements Bus {

    private static final String EMPTY_STRING = "";

    private final Logger logger = LoggerFactory.getLogger(RabbitMQBus.class);
    private final UserContextProvider userContextProvider;
    private final CorrelationIdProvider correlationIdProvider;
    private final Map<UUID, Channel> deadLetterChannels;
    private final Map<String, ProducerInfo> standardProducers;
    private final SerializerProvider serializerProvider;

    @Inject
    public RabbitMQBus(UserContextProvider userContextProvider,
                       CorrelationIdProvider correlationIdProvider,
                       Set<ProducerTopic> producerTopics,
                       RabbitMQConnectionManager connectionManager,
                       SerializerProvider serializerProvider,
                       ServiceConfigurationProvider serviceConfigurationProvider) {

        this.userContextProvider = userContextProvider;
        this.correlationIdProvider = correlationIdProvider;
        this.serializerProvider = serializerProvider;
        List<ProducerInfo> producers = createProducers(producerTopics, connectionManager, serviceConfigurationProvider);

        this.standardProducers = producers.stream()
                .filter(p -> !p.isDeadLetter())
                .collect(Collectors.toUnmodifiableMap(
                        ProducerInfo::key,
                        p -> p
                ));

        this.deadLetterChannels = producers.stream()
                .filter(ProducerInfo::isDeadLetter)
                .collect(Collectors.toUnmodifiableMap(
                        ProducerInfo::tenantId,
                        ProducerInfo::channel
                ));
    }

    private static List<ProducerInfo> createProducers(
            Set<ProducerTopic> producerTopics,
            RabbitMQConnectionManager connectionManager,
            ServiceConfigurationProvider serviceConfigurationProvider) {

        Service currentService = serviceConfigurationProvider.getCurrentService();
        List<ProducerInfo> producers = new ArrayList<>();

        for (Vertical vertical : currentService.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                if (tenant.serviceBusConnectionString() == null ||
                        tenant.serviceBusConnectionString().isBlank()) {
                    continue;
                }

                String connectionName = String.format("%s-PRODUCER-%s-%s",
                        currentService.id(), vertical.id(), tenant.id());

                try {
                    Connection connection = connectionManager.createConnection(
                            tenant.serviceBusConnectionString(), connectionName);

                    for (ProducerTopic topic : producerTopics) {
                        Channel channel = connection.createChannel();
                        channel.exchangeDeclare(topic.getName(), topic.getType(), true);

                        ProducerInfo info = new ProducerInfo(
                                topic.getName(),
                                topic.getType(),
                                channel,
                                tenant.id(),
                                false,
                                topic.getSerializationFormat()
                        );
                        producers.add(info);
                    }

                    Channel dlqChannel = connection.createChannel();
                    ProducerInfo dlqInfo = new ProducerInfo(
                            null, null, dlqChannel, tenant.id(), true, SerializationFormat.JSON
                    );
                    producers.add(dlqInfo);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize RabbitMQ producers", e);
                }
            }
        }

        return producers;
    }

    @Override
    public void send(Command command) {
        sendMessage(command, userContextProvider.getUserContext());
    }

    @Override
    public void send(Command command, UserContext userContext) {
        sendMessage(command, userContext);
    }

    @Override
    public void publish(Event event) {
        sendMessage(event, userContextProvider.getUserContext());
    }

    @Override
    public void publish(Event event, UserContext userContext) {
        sendMessage(event, userContext);
    }

    @Override
    public void publish(boolean exchange, String topic, Map<String, Object> headers, byte[] body) {
        UserContext userContext = userContextProvider.getUserContext();
        Channel channel = deadLetterChannels.get(userContext.getTenantId());

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .headers(headers)
                .deliveryMode(2)
                .build();

        try {
            if (exchange)
                channel.basicPublish(topic, EMPTY_STRING, props, body);
            else
                channel.basicPublish(EMPTY_STRING, topic, props, body);
        } catch (IOException e) {
            throw new RuntimeException("Failed to publish message to RabbitMQ", e);
        }
    }

    private <T extends Message> void sendMessage(T message, UserContext userContext) {

        String topicName = message.getClass().getName();
        String key = ProducerInfo.calculateKey(topicName, userContext.getTenantId());

        ProducerInfo producerInfo = standardProducers.get(key);
        if (producerInfo == null) {
            throw new IllegalStateException(
                    "No producer configured for [" + topicName + "] and tenant [" + userContext.getTenantId() + "]");
        }

        if (message.getUserContext() == null)
            message.setUserContext(userContext);

        if (message.getCorrelationId() == null)
            message.setCorrelationId(correlationIdProvider.getCorrelationId());

        try {
            byte[] body = serialize(message, producerInfo.serializationFormat());
            String routingKey = !Topic.DIRECT.equals(producerInfo.type())
                    ? EMPTY_STRING
                    : message.getUserContext().getVerticalId().toString();

            producerInfo.channel().basicPublish(producerInfo.name(), routingKey, null, body);
        } catch (Exception e) {
            logger.error("Error during message send to exchange [{}]: {}", topicName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private byte[] serialize(Message message, SerializationFormat serializationFormat) {
        return serializerProvider.getSerializer(serializationFormat).serializeToBytes(message);
    }
}