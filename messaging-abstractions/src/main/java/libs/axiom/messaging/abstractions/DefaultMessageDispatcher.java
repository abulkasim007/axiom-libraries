package libs.axiom.messaging.abstractions;

import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.middlewares.authorization.AuthorizationMiddleware;
import libs.axiom.messaging.abstractions.middlewares.infrastructure.MiddlewarePipeline;
import libs.axiom.messaging.abstractions.middlewares.messageHandler.MessageHandlerMiddleware;
import libs.axiom.messaging.abstractions.middlewares.notificationHandler.NotificationHandlerMiddleware;
import libs.axiom.messaging.abstractions.middlewares.resilience.FaultHandlerMiddleware;
import libs.axiom.serialization.abstractions.SerializerProvider;

import java.util.Map;

public class DefaultMessageDispatcher<T extends Message> implements MessageDispatcher {

    private final ConsumerTopic<T> consumerTopic;

    private final MiddlewarePipeline<T> pipeline;
    private final MessageHandler<T> messageHandler;
    private final SerializerProvider serializerProvider;

    @Inject
    public DefaultMessageDispatcher(
            MessageHandler<T> messageHandler,
            FaultHandlerMiddleware<T> faultHandlerMiddleware,
            AuthorizationMiddleware<T> authorizationMiddleware,
            MessageHandlerMiddleware<T> messageHandlerMiddleware,
            NotificationHandlerMiddleware<T> notificationHandlerMiddleware,
            ConsumerTopic<T> consumerTopic,
            SerializerProvider serializerProvider) {
        this.messageHandler = messageHandler;
        this.consumerTopic = consumerTopic;
        this.serializerProvider = serializerProvider;

        this.pipeline = new MiddlewarePipeline.Builder<T>()
                .use(authorizationMiddleware)
                .use(messageHandlerMiddleware)
                .use(faultHandlerMiddleware)
                .use(notificationHandlerMiddleware)
                .build();
    }

    @Override
    public String getTopic() {
        return consumerTopic.getName();
    }

    @Override
    public String getTopicType() {
        return consumerTopic.getType();
    }

    @Override
    public int getPrefetchSize() {
        return consumerTopic.getPrefetchSize();
    }

    @Override
    public void dispatch(long messageId, boolean redeliver, byte[] messageBytes, Map<String, Object> headers) {

        Context<T> context = new Context<>();

        context.setTopic(consumerTopic.getName());
        context.setId(messageId);
        context.setHeaders(headers);
        context.setBody(messageBytes);
        context.setRedeliver(redeliver);
        context.setMessageHandler(messageHandler);

        T message = null;

        try {
            message = serializerProvider.getSerializer(consumerTopic.getSerializationFormat()).deserialize(messageBytes, consumerTopic.getMessageClass());
            context.setMessage(message);
            message.setRedeliver(redeliver);
        } catch (Exception e) {
            context.setFaulted(false, new MalformedPayloadException("Exception on dispatching message from topic: " + consumerTopic.getName(), e));
        }

        ScopedValue.where(ScopedContext.THREAD_LOCAL_CONTEXT, new ThreadLocalContext(message)).run(() -> pipeline.execute(context));
    }
}