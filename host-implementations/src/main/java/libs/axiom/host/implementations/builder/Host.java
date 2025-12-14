package libs.axiom.host.implementations.builder;

import com.google.inject.Injector;
import libs.axiom.host.abstractions.HostedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;

public final class Host {
    private final Injector injector;
    private final ExecutorService executorService;
    private final Set<HostedService> hostedServices;
    private final Logger logger = LoggerFactory.getLogger(Host.class);

    public Host(Injector injector, ExecutorService executorService, Set<HostedService> hostedServices) {
        this.injector = injector;
        this.hostedServices = hostedServices;
        this.executorService = executorService;
    }

    public <T> T getService(Class<T> clazz) {
        return this.injector.getInstance(clazz);
    }

    public void start() {
        try {
            for (HostedService hostedService : hostedServices) {
                hostedService.start();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    this.logger.info("Application is shutting down on CTRL+C.");

                    for (HostedService hostedService : hostedServices) {
                        hostedService.stop();
                    }
                    this.executorService.close();
                } catch (Exception e) {
                    this.logger.error(e.getMessage(), e);
                }
            }));
            this.logger.info("Application is running. Press Ctrl+C to stop...");
            Thread.currentThread().join();
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }
    }
}

