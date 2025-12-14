package libs.axiom.messaging.abstractions;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Types;
import libs.axiom.messaging.abstractions.middlewares.authorization.AuthorizationMiddleware;
import libs.axiom.messaging.abstractions.middlewares.messageHandler.MessageHandlerMiddleware;
import libs.axiom.messaging.abstractions.middlewares.notificationHandler.NotificationHandlerMiddleware;
import libs.axiom.messaging.abstractions.middlewares.resilience.FaultHandlerMiddleware;
import libs.axiom.serialization.abstractions.SerializationFormat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MessageListeners {
    private final Binder binder;

    private MessageListeners(Binder binder) {
        this.binder = binder;
    }

    public static MessageListeners from(Binder binder) {
        return new MessageListeners(binder);
    }

    @SuppressWarnings("unchecked")
    public <M extends Message, H extends MessageHandler<M>> MessageListeners add(Class<H> messageHandlerType, SerializationFormat serializationFormat, int prefetchSize) {

        Class<M> messageType = (Class<M>) ((ParameterizedType) messageHandlerType.getGenericInterfaces()[0]).getActualTypeArguments()[0];

        // Message handler
        var messageHandlerInterfaceType = Types.newParameterizedType(MessageHandler.class, messageType);

        binder.bind((TypeLiteral<MessageHandler<M>>) TypeLiteral.get(messageHandlerInterfaceType))
                .to(messageHandlerType);

        // Middlewares
        var middlewareMultibinder = Multibinder.newSetBinder(
                binder,
                (TypeLiteral<Middleware<M>>) TypeLiteral.get(
                        Types.newParameterizedType(Middleware.class, messageType)
                )
        );

        middlewareMultibinder.addBinding().to(
                (TypeLiteral<FaultHandlerMiddleware<M>>) TypeLiteral.get(
                        Types.newParameterizedType(FaultHandlerMiddleware.class, messageType)
                )
        );

        middlewareMultibinder.addBinding().to(
                (TypeLiteral<AuthorizationMiddleware<M>>) TypeLiteral.get(
                        Types.newParameterizedType(AuthorizationMiddleware.class, messageType)
                )
        );

        middlewareMultibinder.addBinding().to(
                (TypeLiteral<MessageHandlerMiddleware<M>>) TypeLiteral.get(
                        Types.newParameterizedType(MessageHandlerMiddleware.class, messageType)
                )
        );

        middlewareMultibinder.addBinding().to(
                (TypeLiteral<NotificationHandlerMiddleware<M>>) TypeLiteral.get(
                        Types.newParameterizedType(NotificationHandlerMiddleware.class, messageType)
                )
        );

        // Consumer topic
        var consumerTopicType = Types.newParameterizedType(ConsumerTopic.class, messageType);
        binder.bind((TypeLiteral<ConsumerTopic<M>>) TypeLiteral.get(consumerTopicType))
                .toInstance(new ConsumerTopic<>(messageType.getTypeName(), Topic.FANOUT, prefetchSize, serializationFormat, messageType));

        // Message dispatcher
        Type dispatcherGenericType = Types.newParameterizedType(DefaultMessageDispatcher.class, messageType);
        TypeLiteral<DefaultMessageDispatcher<M>> dispatcherTypeLiteral =
                (TypeLiteral<DefaultMessageDispatcher<M>>) TypeLiteral.get(dispatcherGenericType);

        Multibinder<MessageDispatcher> globalDispatcherMultibinder =
                Multibinder.newSetBinder(binder, MessageDispatcher.class);
        globalDispatcherMultibinder.addBinding().to(dispatcherTypeLiteral);

        return this;
    }
}

