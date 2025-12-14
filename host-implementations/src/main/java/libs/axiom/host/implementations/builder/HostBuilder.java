package libs.axiom.host.implementations.builder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.inject.Singleton;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.host.abstractions.HostedService;
import libs.axiom.host.implementations.extensions.*;
import libs.axiom.host.implementations.serializers.DefaultSerializerProvider;
import libs.axiom.messaging.abstractions.CorrelationIdProvider;
import libs.axiom.messaging.abstractions.MessageProvider;
import libs.axiom.messaging.abstractions.ThreadLocalContextProvider;
import libs.axiom.messaging.abstractions.UserContextProvider;
import libs.axiom.serialization.abstractions.SerializerProvider;
import libs.axiom.threading.abstractions.ExecutorServiceProvider;
import org.slf4j.simple.SimpleLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HostBuilder extends AbstractModule {

    private final SerializerProvider serializerProvider;
    private final ExecutorServiceProvider executorServiceProvider;
    private final ServiceConfigurationProvider serviceConfigurationProvider;
    private final List<AbstractModule> userProvidedModules = new ArrayList<>();

    public HostBuilder() {

        Config configuration = ConfigFactory.load();

        Config serviceConfigurationConfig = configuration.getConfig(Service.SERVICE_CONFIGURATION_KEY);

        Map<String, Object> serviceConfigurationMap = serviceConfigurationConfig.root().unwrapped();

        this.serializerProvider = new DefaultSerializerProvider();

        this.serviceConfigurationProvider =
                new DefaultServiceConfigurationProvider(this.serializerProvider, serviceConfigurationMap);

        Service service = serviceConfigurationProvider.getCurrentService();

        this.executorServiceProvider = new VirtualThreadExecutorServiceProvider(serviceConfigurationProvider);

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, service.logLevel());
    }

    @Override
    protected void configure() {
        bind(SerializerProvider.class).toInstance(this.serializerProvider);
        bind(ExecutorServiceProvider.class).toInstance(this.executorServiceProvider);
        bind(ServiceConfigurationProvider.class).toInstance(this.serviceConfigurationProvider);
        bind(MessageProvider.class).to(DefaultMessageProvider.class).in(Singleton.class);
        bind(UserContextProvider.class).to(DefaultUserContextProvider.class).in(Singleton.class);
        bind(CorrelationIdProvider.class).to(DefaultCorrelationIdProvider.class).in(Singleton.class);
        bind(ThreadLocalContextProvider.class).to(DefaultThreadLocalContextProvider.class).in(Singleton.class);

        Multibinder.newSetBinder(binder(), HostedService.class);
    }

    public HostBuilder addModule(AbstractModule module) {
        this.userProvidedModules.add(module);
        return this;
    }

    public Host build() {
        userProvidedModules.add(this);
        Injector injector = Guice.createInjector(userProvidedModules);
        Set<HostedService> hostedServices = injector.getInstance(new Key<>() {
        });
        return new Host(injector, this.executorServiceProvider.getExecutorService(), hostedServices);
    }
}