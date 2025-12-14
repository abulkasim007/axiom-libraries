package libs.axiom.messaging.rabbitmq.implementations.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.host.abstractions.HostedService;
import libs.axiom.messaging.abstractions.MessageDispatcher;
import libs.axiom.messaging.rabbitmq.implementations.connection.RabbitMQConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RabbitMQMessageListener implements HostedService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageListener.class);
    private static final Map<String, Object> DELAYED_EXCHANGE_HEADERS = Map.of("x-delayed-type", "direct");

    private final List<RabbitMQConsumer> consumers = new ArrayList<>();

    private final Set<MessageDispatcher> messageDispatchers;
    private final RabbitMQConnectionManager connectionManager;
    private final ServiceConfigurationProvider serviceConfigurationProvider;

    @Inject
    public RabbitMQMessageListener(Set<MessageDispatcher> messageDispatchers, RabbitMQConnectionManager connectionManager, ServiceConfigurationProvider serviceConfigurationProvider) {
        this.connectionManager = connectionManager;
        this.messageDispatchers = messageDispatchers;
        this.serviceConfigurationProvider = serviceConfigurationProvider;
    }

    // Equivalent to StartAsync
    public void start() {
        Service service = serviceConfigurationProvider.getCurrentService();

        for (Vertical vertical : service.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                String connectionString = tenant.serviceBusConnectionString();
                if (connectionString == null || connectionString.trim().isEmpty()) {
                    continue;
                }

                String connectionName = String.format("%s-CONSUMER-%s-%s",
                        service.id(), vertical.id(), tenant.id());

                Connection connection;
                try {
                    connection = connectionManager.createConnection(connectionString, connectionName);
                } catch (NoSuchAlgorithmException | KeyManagementException e) {
                    throw new RuntimeException(e);
                }


                for (MessageDispatcher dispatcher : messageDispatchers) {
                    try {
                        Channel channel = connection.createChannel();
                        setupChannel(service, channel, dispatcher);
                    } catch (IOException e) {
                        logger.error("Failed to create channel for dispatcher: {}", dispatcher.getTopic(), e);
                    }
                }
            }
        }
    }


    @Override
    public void stop() {
        for (RabbitMQConsumer consumer : consumers) {
            try {
                consumer.stop();
                logger.info("Channel of topic {} closed cleanly", consumer.getChannelQueueName());
            } catch (Exception e) {
                logger.error("Error stopping RabbitMQ consumer for topic {}", consumer.getChannelQueueName(), e);
            }
        }
    }

    private void setupChannel(Service service, Channel channel, MessageDispatcher dispatcher) throws IOException {
        String messageQueueName = dispatcher.getTopic() + "_" + service.id();

        // Declare main queue
        channel.queueDeclare(messageQueueName, true, false, false, null);

        // Declare exchange
        channel.exchangeDeclare(dispatcher.getTopic(), dispatcher.getTopicType(), true);

        // Bind queue to exchange
        channel.queueBind(messageQueueName, dispatcher.getTopic(), "");

        // Declare dead-letter (error) queue
        String deadLetterQueueName = messageQueueName + "_Error";
        channel.queueDeclare(deadLetterQueueName, true, false, false, null);

        setupRetry(messageQueueName, channel);

        int prefetchCount = dispatcher.getPrefetchSize();
        channel.basicQos(prefetchCount);

        String consumerTag = java.util.UUID.randomUUID().toString();

        RabbitMQConsumer asyncConsumer = new RabbitMQConsumer(logger, channel, messageQueueName, consumerTag, dispatcher);

        channel.basicConsume(messageQueueName, false, consumerTag, asyncConsumer);

        consumers.add(asyncConsumer);

        logger.info("Gen-Z Dispatcher started for queue: {} with prefetch size: {}", messageQueueName, prefetchCount);
    }

    private void setupRetry(String consumerQueueName, Channel channel) throws IOException {
        String retryExchangeName = consumerQueueName + "_Retry";
        channel.exchangeDeclare(retryExchangeName, "x-delayed-message", true, false, DELAYED_EXCHANGE_HEADERS);
        channel.queueBind(consumerQueueName, retryExchangeName, "");
    }
}