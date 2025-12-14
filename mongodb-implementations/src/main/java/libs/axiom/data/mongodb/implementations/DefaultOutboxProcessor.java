package libs.axiom.data.mongodb.implementations;

import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.data.abstractions.DatabaseType;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.RepositoryCache;
import libs.axiom.data.abstractions.reliability.OutboxImpl;
import libs.axiom.data.abstractions.reliability.OutboxProcessor;
import libs.axiom.data.abstractions.reliability.QOutboxImpl;
import libs.axiom.data.abstractions.utils.AutoCloseableIterator;
import libs.axiom.messaging.abstractions.Bus;
import libs.axiom.messaging.abstractions.Event;
import libs.axiom.messaging.abstractions.NotifiedByAggregateRoot;
import libs.axiom.messaging.abstractions.NotifyCommand;
import libs.axiom.messaging.abstractions.transaction.Outbox;
import libs.axiom.messaging.abstractions.transaction.OutboxStatus;
import libs.axiom.threading.abstractions.ExecutorServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class DefaultOutboxProcessor implements OutboxProcessor {

    private static final int MAX_ATTEMPTS = 0;

    private static final QOutboxImpl qOutbox = QOutboxImpl.outboxImpl;

    private static final Semaphore permits = new Semaphore(10);

    private static final Logger logger = LoggerFactory.getLogger(DefaultOutboxProcessor.class.getName());

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("outbox-retry-scheduler-vt-", 0).factory());

    private final Bus bus;
    private final ExecutorService executor;
    private final RepositoryCache repositoryCache;
    private final ServiceConfigurationProvider serviceConfigurationProvider;

    @Inject
    public DefaultOutboxProcessor(Bus bus, RepositoryCache repositoryCache, ServiceConfigurationProvider serviceConfigurationProvider, ExecutorServiceProvider executorServiceProvider) {
        this.bus = bus;
        this.repositoryCache = repositoryCache;
        this.executor = executorServiceProvider.getExecutorService();
        this.serviceConfigurationProvider = serviceConfigurationProvider;
    }

    @Override
    public void start() {

        Service service = serviceConfigurationProvider.getCurrentService();
        for (Vertical vertical : service.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                logger.info("Loading pending outboxes for tenant: {}", tenant.id());
                Repository repository = repositoryCache.getTenantRepository(DatabaseType.STATE, tenant.id(), tenant.id());
                try (AutoCloseableIterator<OutboxImpl> outboxes = repository.getIterator(qOutbox.outboxStatus.ne(OutboxStatus.CLEARED), OutboxImpl.class, Outbox.class.getSimpleName() + "s")) {
                    while (outboxes.hasNext()) {
                        Outbox outbox = outboxes.next();
                        outbox.setLoaded(true);
                        outbox.setPersistent(true);
                        submit(outbox);
                    }
                }
                logger.info("Finished loading pending outboxes for tenant: {}", tenant.id());
            }
        }

    }

    @Override
    public void stop() {
        scheduler.shutdown();
    }


    private void saveEvents(Outbox outbox) {
        if (outbox.getEvents().isEmpty()) {
            return;
        }

        if (!outbox.isLoaded()) {
            repositoryCache
                    .getTenantRepository(DatabaseType.EVENT, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                    .save(outbox.getEvents(), Event.class);

            return;
        }

        if (outbox.getEvents().size() == 1) {
            Event event = outbox.getEvents().getFirst();

            boolean exists = repositoryCache
                    .getTenantRepository(DatabaseType.EVENT, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                    .exists(qOutbox.id.eq(event.getId()), Event.class);

            if (exists) {
                return;
            }

            repositoryCache
                    .getTenantRepository(DatabaseType.EVENT, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                    .save(event, Event.class);

            return;

        }

        List<UUID> eventIds = outbox.getEvents().stream().map(Event::getId).toList();

        List<UUID> existingEventIds = repositoryCache
                .getTenantRepository(DatabaseType.EVENT, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                .getMany(eventIds, Event.class)
                .stream()
                .map(Event::getId).toList();

        if (existingEventIds.size() == eventIds.size()) {
            return;
        }

        List<Event> nonExistingEvents = outbox.getEvents().stream().filter(e -> !existingEventIds.contains(e.getId())).toList();

        repositoryCache
                .getTenantRepository(DatabaseType.EVENT, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                .save(nonExistingEvents, Event.class);
    }


    private void updateOutbox(Outbox outbox) {

        outbox.setOutboxStatus(OutboxStatus.CLEARED);

        repositoryCache
                .getTenantRepository(DatabaseType.STATE, outbox.getUserContext().getTenantId(), outbox.getUserContext().getVerticalId())
                .update(qOutbox.id.eq(outbox.getId()), outbox, Outbox.class);
    }


    private void publishEvents(Outbox outbox) {
        for (Event ev : outbox.getEvents()) {
            bus.publish(ev, outbox.getUserContext());

            if (!(ev instanceof NotifiedByAggregateRoot notificationEvent)) {
                continue;
            }

            NotifyCommand notifyCommand = new NotifyCommand();

            notifyCommand.setUserContext(ev.getUserContext());
            notifyCommand.setCorrelationId(ev.getCorrelationId());
            notifyCommand.setRoles(notificationEvent.getRoles());
            notifyCommand.setTopic(notificationEvent.getTopic());
            notifyCommand.setOffline(notificationEvent.isOffline());
            notifyCommand.setUserIds(notificationEvent.getUserIds());
            notifyCommand.setDevices(notificationEvent.getDevices());
            notifyCommand.setMessage(notificationEvent.getMessage());
            notifyCommand.setSessionIds(notificationEvent.getSessionIds());

            bus.send(notifyCommand, outbox.getUserContext());
        }
    }

    @Override
    public void submit(Outbox outbox) {

        try {
            permits.acquire();
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for permits", e);
        }
        executor.execute(() -> {
            try {

                logger.info("Start processing outbox id: {} at attempt: {}", outbox.getId(), outbox.getAttempt());

                // Saving events
                saveEvents(outbox);
                // Publishing events
                publishEvents(outbox);
                // Updating outbox
                updateOutbox(outbox);

                logger.info("Success processing outbox id: {} at attempt: {}", outbox.getId(), outbox.getAttempt());
            } catch (Exception e) {
                if (outbox.getAttempt() < MAX_ATTEMPTS) {

                    logger.warn("Attempt {} failed: {}. Retrying in {} ms...", outbox.getAttempt(), e.getMessage(), 2000);

                    outbox.updateAttempts();

                    scheduler.schedule(() -> submit(outbox), 2000, TimeUnit.MILLISECONDS);
                } else {
                    logger.error("Failed processing outbox id: {} at attempt: {} error: {}", outbox.getId(), outbox.getAttempt(), e.getMessage());
                }
            } finally {
                permits.release();
            }
        });
    }
}
