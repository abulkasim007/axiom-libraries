package libs.axiom.messaging.abstractions.middlewares.messageHandler;

import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.*;
import libs.axiom.messaging.abstractions.transaction.OutboxManager;
import libs.axiom.messaging.abstractions.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandlerMiddleware<T extends Message> implements Middleware<T> {

    private final ThreadLocalContextProvider threadLocalContextProvider;

    private static final Logger logger = LoggerFactory.getLogger(MessageHandlerMiddleware.class);

    @Inject
    public MessageHandlerMiddleware(ThreadLocalContextProvider threadLocalContextProvider) {
        this.threadLocalContextProvider = threadLocalContextProvider;
    }

    @Override
    public void invoke(Context<T> context, PipelineFunction<T> next) {
        if (context.isFaulted()) {
            next.apply(context);
            return;
        }

        java.util.UUID correlationId = context.getMessage().getCorrelationId();

        if (logger.isInfoEnabled()) {
            if (context.isRetry()) {
                logger.info("Retrying: {} with correlation id: {}", context.getTopic(), correlationId);
            } else {
                logger.info("Beginning handling message type: {} with correlation id: {}", context.getTopic(), correlationId);
            }
        }

        long startTimeNanos = -1;
        if (logger.isInfoEnabled()) {
            startTimeNanos = System.nanoTime();
        }

        ThreadLocalContext threadLocalContext = threadLocalContextProvider.getThreadLocalContext();

        try {
            MessageHandler<T> handler = context.getMessageHandler();
            handler.handle(context.getMessage());

            TransactionManager transactionManager = threadLocalContext.getFeatureOfBaseType(TransactionManager.class);

            if (transactionManager != null) {
                try {
                    transactionManager.commit();
                } catch (Exception commitException) {
                    context.setFaulted(true, commitException);
                    try (transactionManager) {
                        if (logger.isWarnEnabled())
                            logger.warn("Rolling back transaction: {}", transactionManager.name(), commitException);
                        transactionManager.rollback();
                    } catch (Exception rollbackException) {
                        logger.error("Rollback failed for {}: {}", transactionManager.name(), rollbackException.getMessage(), rollbackException);
                    }
                }
            }
        } catch (Exception e) {
            context.setFaulted(true, e);
        }

        OutboxManager outboxManager = threadLocalContext.getFeatureOfBaseType(OutboxManager.class);

        if (outboxManager != null) {
            try {
                outboxManager.submit();
            } catch (Exception e) {
                context.setFaulted(true, e);
            }
        }

        next.apply(context);

        if (logger.isInfoEnabled() && startTimeNanos != -1) {
            long durationNanos = System.nanoTime() - startTimeNanos;
            double durationMs = durationNanos / 1_000_000.0;
            logger.info("End handling message type: {} with correlation id: {}. Took {} ms.",
                    context.getTopic(), correlationId, durationMs);
        }
    }
}