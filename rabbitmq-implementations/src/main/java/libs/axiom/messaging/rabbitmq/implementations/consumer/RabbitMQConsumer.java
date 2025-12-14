package libs.axiom.messaging.rabbitmq.implementations.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.MessageDispatcher;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

public class RabbitMQConsumer extends DefaultConsumer {

    private final Logger logger;
    private final String queueName;
    private final String consumerTag;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public RabbitMQConsumer(
            Logger logger,
            Channel channel,
            String queueName,
            String consumerTag,
            MessageDispatcher messageDispatcher) {
        super(channel);
        this.logger = logger;
        this.queueName = queueName;
        this.consumerTag = consumerTag;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {

        try {
            Map<String, Object> headers = properties == null ? null : properties.getHeaders();
            messageDispatcher.dispatch(envelope.getDeliveryTag(), envelope.isRedeliver(), body, headers);
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error("Error processing message from exchange {} (deliveryTag {})", envelope.getExchange(), envelope.getDeliveryTag(), e);

            try {
                getChannel().basicNack(envelope.getDeliveryTag(), false, true); // requeue
            } catch (IOException nackEx) {
                logger.error("Error sending NACK for delivery tag {} on queue {}",
                        envelope.getDeliveryTag(), getChannelQueueName(), nackEx);
            }
        }
    }

    public void stop() {
        try {
            getChannel().basicCancel(consumerTag);
        } catch (IOException e) {
            logger.error("Error cancelling consumer on queue {}", getChannelQueueName(), e);
        }
    }

    public String getChannelQueueName() {
        return this.queueName;
    }
}