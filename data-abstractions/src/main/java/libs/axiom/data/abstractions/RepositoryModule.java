package libs.axiom.data.abstractions;


import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import jakarta.inject.Singleton;
import libs.axiom.data.abstractions.proxy.DefaultRepositoryProxy;
import libs.axiom.data.abstractions.proxy.ProxiedEventRepository;
import libs.axiom.data.abstractions.proxy.ProxiedReadRepository;
import libs.axiom.data.abstractions.proxy.ProxiedStateRepository;
import libs.axiom.data.abstractions.reliability.DefaultOutboxProcessorHostedService;
import libs.axiom.host.abstractions.HostedService;
import libs.axiom.messaging.abstractions.Bus;
import libs.axiom.messaging.abstractions.NoOpBus;

public class RepositoryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RepositoryProxy.class)
                .to(DefaultRepositoryProxy.class)
                .in(Singleton.class);

        bind(StateRepository.class)
                .to(ProxiedStateRepository.class)
                .in(Singleton.class);

        bind(EventRepository.class)
                .to(ProxiedEventRepository.class)
                .in(Singleton.class);

        bind(ReadRepository.class)
                .to(ProxiedReadRepository.class)
                .in(Singleton.class);




        Multibinder<HostedService> mb = Multibinder.newSetBinder(binder(), HostedService.class);
        mb.addBinding().to(DefaultOutboxProcessorHostedService.class).in(Singleton.class);

        OptionalBinder.newOptionalBinder(binder(), Bus.class).setDefault().to(NoOpBus.class).in(Singleton.class);
    }
}

