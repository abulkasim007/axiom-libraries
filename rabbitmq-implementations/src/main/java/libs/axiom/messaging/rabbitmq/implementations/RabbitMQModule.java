package libs.axiom.messaging.rabbitmq.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import jakarta.inject.Singleton;
import libs.axiom.host.abstractions.HostedService;
import libs.axiom.messaging.abstractions.Bus;
import libs.axiom.messaging.abstractions.MessagePublishers;
import libs.axiom.messaging.abstractions.NotifyCommand;
import libs.axiom.messaging.rabbitmq.implementations.connection.RabbitMQConnectionManager;
import libs.axiom.messaging.rabbitmq.implementations.consumer.RabbitMQMessageListener;
import libs.axiom.messaging.rabbitmq.implementations.producer.RabbitMQBus;
import libs.axiom.serialization.abstractions.SerializationFormat;

public class RabbitMQModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(RabbitMQConnectionManager.class).in(Singleton.class);

        MessagePublishers.from(binder()).add(NotifyCommand.class, SerializationFormat.JSON);

        Multibinder<HostedService> mb = Multibinder.newSetBinder(binder(), HostedService.class);
        mb.addBinding().to(RabbitMQMessageListener.class).in(Singleton.class);

        OptionalBinder.newOptionalBinder(binder(), Bus.class).setBinding().to(RabbitMQBus.class).in(Singleton.class);
    }
}
