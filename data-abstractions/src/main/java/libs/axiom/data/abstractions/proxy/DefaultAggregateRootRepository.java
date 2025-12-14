package libs.axiom.data.abstractions.proxy;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.inject.Inject;
import libs.axiom.data.abstractions.AggregateRootRepository;
import libs.axiom.data.abstractions.StateRepository;
import libs.axiom.data.abstractions.configuration.AggregateTypeReference;
import libs.axiom.data.abstractions.exceptions.ConcurrencyException;
import libs.axiom.data.abstractions.models.AggregateRoot;
import libs.axiom.data.abstractions.reliability.DefaultOutboxManager;
import libs.axiom.data.abstractions.reliability.OutboxConfig;
import libs.axiom.data.abstractions.reliability.OutboxImpl;
import libs.axiom.data.abstractions.reliability.OutboxProcessor;
import libs.axiom.messaging.abstractions.*;
import libs.axiom.messaging.abstractions.transaction.Outbox;
import libs.axiom.messaging.abstractions.transaction.OutboxManager;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefaultAggregateRootRepository<T extends AggregateRoot> implements AggregateRootRepository<T> {

    private final Bus bus;
    private final Class<T> type;
    private final OutboxConfig outboxConfig;
    private final StateRepository stateRepository;
    private final OutboxProcessor outboxProcessor;
    private final UserContextProvider userContextProvider;
    private final CorrelationIdProvider correlationIdProvider;
    private final ThreadLocalContextProvider threadLocalContextProvider;

    @Inject
    public DefaultAggregateRootRepository(AggregateTypeReference<T> aggregateTypeReference, Bus bus, StateRepository stateRepository, OutboxConfig outboxConfig, OutboxProcessor outboxProcessor, UserContextProvider userContextProvider, CorrelationIdProvider correlationIdProvider, ThreadLocalContextProvider threadLocalContextProvider) {
        this.bus = bus;
        this.outboxConfig = outboxConfig;
        this.stateRepository = stateRepository;
        this.outboxProcessor = outboxProcessor;
        this.type = aggregateTypeReference.getType();
        this.userContextProvider = userContextProvider;
        this.correlationIdProvider = correlationIdProvider;
        this.threadLocalContextProvider = threadLocalContextProvider;
    }

    public T get(UUID id) {
        return stateRepository.get(id, type);
    }

    public T get(Predicate filter) {
        return stateRepository.get(filter, type);
    }

    public void delete(Predicate filter) {
        stateRepository.delete(filter, type);
    }

    public boolean exists(Predicate filter) {
        return stateRepository.exists(filter, type);
    }

    public List<T> getMany(Predicate filter) {
        return stateRepository.getMany(filter, type);
    }

    public void save(T aggregate) {

        PrepareAggregate(aggregate);

        Optional<Event> businessRuleViolatedEvent = validate(aggregate);

        if (businessRuleViolatedEvent.isPresent()) {
            SendNotification(businessRuleViolatedEvent.get());
            return;
        }

        if (!aggregate.getOutbox().isPersistent()) {
            stateRepository.save(aggregate, type);
            return;
        }

        stateRepository.save(aggregate, type);
        stateRepository.save(aggregate.getOutbox(), Outbox.class);

        OutboxManager outboxManager = new DefaultOutboxManager(aggregate.getOutbox(), this.outboxProcessor);

        this.threadLocalContextProvider.getThreadLocalContext().setFeature(outboxManager);
    }

    public void update(T aggregate) {
        int expectedVersion = aggregate.getVersion();

        PrepareAggregate(aggregate);

        Optional<Event> businessRuleViolatedEvent = validate(aggregate);

        if (businessRuleViolatedEvent.isPresent()) {
            SendNotification(businessRuleViolatedEvent.get());
            return;
        }

        if (!aggregate.getOutbox().isPersistent()) {
            stateRepository.update(new BooleanBuilder(), aggregate, type);
            return;
        }

        long updateCount = stateRepository.update(aggregate, aggregate.getId(), expectedVersion, type);

        if (updateCount == 0) {
            throw new ConcurrencyException(aggregate.getId(), expectedVersion, aggregate.getVersion());
        }

        stateRepository.save(aggregate.getOutbox(), Outbox.class);

        OutboxManager outboxManager = new DefaultOutboxManager(aggregate.getOutbox(), this.outboxProcessor);

        this.threadLocalContextProvider.getThreadLocalContext().setFeature(outboxManager);
    }

    private void PrepareAggregate(AggregateRoot aggregate) {
        UUID correlationId = correlationIdProvider.getCorrelationId();
        UserContext userContext = userContextProvider.getUserContext();

        aggregate.assignEntityDefaults(userContext);

        List<Event> aggregateEvents = aggregate.getEvents();

        EnrichEvents(aggregateEvents, type, aggregate.getId(), aggregate.getVersion(), correlationId, userContext);

        aggregate.setOutbox(OutboxImpl.from(type, aggregate.getId(), aggregate.getVersion(), aggregate.getEvents(), userContext, correlationId, outboxConfig.transactionalOutboxRequired()));
    }

    private Optional<Event> validate(AggregateRoot aggregate) {

        if (aggregate.getId() == null) {
            throw new IllegalStateException("Aggregate ID must not be null");
        }

        return aggregate.getEvents().stream()
                .filter(event -> event instanceof BusinessRuleViolatedEvent)
                .findFirst();
    }

    private void SendNotification(Event event) {
        NotificationMessage notificationMessage = (NotifiedByAggregateRoot) event;

        NotifyCommand notifyCommand = new NotifyCommand();

        notifyCommand.setUserContext(event.getUserContext());
        notifyCommand.setCorrelationId(event.getCorrelationId());
        notifyCommand.setRoles(notificationMessage.getRoles());
        notifyCommand.setTopic(notificationMessage.getTopic());
        notifyCommand.setOffline(notificationMessage.isOffline());
        notifyCommand.setUserIds(notificationMessage.getUserIds());
        notifyCommand.setDevices(notificationMessage.getDevices());
        notifyCommand.setMessage(notificationMessage.getMessage());
        notifyCommand.setSessionIds(notificationMessage.getSessionIds());

        bus.send(notifyCommand);
    }

    private static <T> void EnrichEvents(List<Event> events, Class<T> aggregateType, UUID entityId, int entityVersion, UUID correlationId, UserContext userContext) {
        Date timeStamp = new Date();

        for (Event e : events) {
            e.setId(UUID.randomUUID());
            e.setUserContext(userContext);
            e.setId(UUID.randomUUID());
            e.setSource(aggregateType.getName());
            e.setTimeStamp(timeStamp);
            e.setAggregateRootId(entityId);
            e.setName(e.getClass().getName());
            e.setAggregateRootVersion(entityVersion);
            e.setCorrelationId(correlationId);
        }
    }
}
