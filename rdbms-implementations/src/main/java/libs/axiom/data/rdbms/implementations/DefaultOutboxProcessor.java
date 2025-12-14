package libs.axiom.data.rdbms.implementations;

import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.Service;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.configuration.abstractions.Tenant;
import libs.axiom.configuration.abstractions.Vertical;
import libs.axiom.data.abstractions.reliability.OutboxImpl;
import libs.axiom.data.abstractions.reliability.OutboxProcessor;
import libs.axiom.data.rdbms.implementations.repositories.OutboxRepository;
import libs.axiom.data.rdbms.implementations.repositories.OutboxRepositoryFactory;
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

    private static final Semaphore permits = new Semaphore(10);

    private static final Logger logger = LoggerFactory.getLogger(DefaultOutboxProcessor.class.getName());

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("outbox-retry-scheduler-vt-", 0).factory());

    private final Bus bus;
    private final ExecutorService executor;
    private final OutboxRepositoryFactory outboxRepositoryFactory;
    private final ServiceConfigurationProvider serviceConfigurationProvider;


    @Inject
    public DefaultOutboxProcessor(Bus bus, OutboxRepositoryFactory outboxRepositoryFactory, ServiceConfigurationProvider serviceConfigurationProvider, ExecutorServiceProvider executorServiceProvider) {
        this.bus = bus;
        this.outboxRepositoryFactory = outboxRepositoryFactory;
        this.executor = executorServiceProvider.getExecutorService();
        this.serviceConfigurationProvider = serviceConfigurationProvider;
    }

    @Override
    public void start() {

        Service service = serviceConfigurationProvider.getCurrentService();
        for (Vertical vertical : service.verticals()) {
            for (Tenant tenant : vertical.tenants()) {
                processLoadedOutboxes(tenant.id());
            }
        }

    }

    @Override
    public void stop() {
        scheduler.shutdown();
    }


    private void processLoadedOutboxes(UUID tenantId) {
        logger.info("Start processing pending outboxes for tenant: {}", tenantId);

        while (true) {

            OutboxRepository outboxRepository = this.outboxRepositoryFactory.getOutboxRepository(tenantId, tenantId);

            List<OutboxImpl> outboxes = outboxRepository.getOutboxes(OutboxStatus.PENDING, 100);

            if (outboxes.isEmpty()) {
                break;
            }

            for (OutboxImpl outbox : outboxes) {

                List<UUID> eventIds = outbox.getEvents().stream().map(Event::getId).toList();

                List<UUID> savedEventIds = outboxRepository.getSavedEventIds(eventIds);

                if (eventIds.size() != savedEventIds.size()) {

                    List<Event> nonExistingEvents = outbox.getEvents().stream().filter(e -> !savedEventIds.contains(e.getId())).toList();

                    outboxRepository.saveEvents(nonExistingEvents);
                }

                // No DB
                publishEvents(outbox);

                // Update Outbox
                outbox.setOutboxStatus(OutboxStatus.CLEARED);
            }

            outboxRepository.saveChanges();
        }

        logger.info("Finished processing pending outboxes for tenant: {}", tenantId);
    }

    private void processOutbox(Outbox outbox) {

        OutboxRepository outboxRepository = this.outboxRepositoryFactory.getOutboxRepository(outbox.getUserContext().getTenantId(), outbox.getUserContext().getTenantId());

        outboxRepository.saveEvents(outbox.getEvents());

        publishEvents(outbox);

        OutboxImpl outboxImpl = outboxRepository.getOutbox(outbox.getId());

        outboxImpl.setOutboxStatus(OutboxStatus.CLEARED);

        outboxRepository.saveChanges();
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

                processOutbox(outbox);

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
