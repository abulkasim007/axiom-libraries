package libs.axiom.host.implementations.extensions;

import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.threading.abstractions.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadExecutorServiceProvider implements ExecutorServiceProvider {
    private final ExecutorService executorService;

    public VirtualThreadExecutorServiceProvider(ServiceConfigurationProvider serviceConfigurationProvider) {

        Service service = serviceConfigurationProvider.getCurrentService();

        ThreadFactory virtualThreadFactory = Thread.ofVirtual().name(service.id() + "-", 0).factory();

        this.executorService = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
    }

    @Override
    public ExecutorService getExecutorService() {
        return this.executorService;
    }
}
