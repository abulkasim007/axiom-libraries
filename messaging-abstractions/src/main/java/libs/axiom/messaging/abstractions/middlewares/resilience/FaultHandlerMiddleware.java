package libs.axiom.messaging.abstractions.middlewares.resilience;

import jakarta.inject.Inject;
import libs.axiom.configuration.abstractions.ServiceConfigurationProvider;
import libs.axiom.messaging.abstractions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FaultHandlerMiddleware<T extends Message> implements Middleware<T> {

    private static final String DELAY_HEADER_NAME = "x-delay";
    private static final Logger logger = LoggerFactory.getLogger(FaultHandlerMiddleware.class);

    private final Bus bus;
    private final String serviceName;
    private final Map<String, Retry> retries;

    @Inject
    public FaultHandlerMiddleware(
            Bus bus,
            Set<Retry> retries,
            ServiceConfigurationProvider serviceConfigurationProvider) {

        this.bus = bus;
        this.serviceName = serviceConfigurationProvider.getCurrentService().id();


        Map<String, Retry> retryMap = new HashMap<>();
        for (Retry retry : retries) {
            retryMap.put(retry.getFault(), retry);
        }
        this.retries = new HashMap<>(retryMap);
    }

    @Override
    public void invoke(Context<T> context, PipelineFunction<T> next) {
        if (!context.isFaulted()) {
            next.apply(context);
            return;
        }

        if (!context.isRecoverableFault()) {
            logUnhandledException(context.getFault(), context);
            return;
        }

        handleFault(context.getFault(), context);
        next.apply(context);
    }

    private void logUnhandledException(Exception exception, Context<T> context) {
        String message = new String(context.getBody(), StandardCharsets.UTF_8);
        logger.error(
                "Unhandled exception {} thrown while processing message of {} payload: {}",
                exception.getMessage(),
                context.getTopic(),
                message,
                exception
        );
    }

    private void handleFault(Exception exception, Context<T> context) {
        try {

            Retry retry = retries.get(exception.getClass().getName());

            if (retry == null) {
                sendToDeadLetterQueue(0, context.getTopic(), context.getMessage().getCorrelationId(), exception, context);
                return;
            }

            int currentRetryAttempt = getCurrentRetryAttempt(context);

            if ( currentRetryAttempt >= retry.getAttempts()) {
                sendToDeadLetterQueue(retry.getAttempts(), context.getTopic(), context.getMessage().getCorrelationId(), exception, context);
                return;
            }

            scheduleRetry(retry.getFault(), retry.getDelay(), currentRetryAttempt, context);

        } catch (Exception e) {
            logger.error("Error handling fault: {}", e.getMessage(), e);
        }
    }

    private int getCurrentRetryAttempt(Context<T> context) {

        Map<String, Object> headers = context.getHeaders();

        if (headers == null) {
            return 0;
        }

        Object value = headers.get(Retry.RETRY_ATTEMPTS_HEADER_NAME);

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void scheduleRetry(String fault, Object delay, int currentRetryAttempt, Context<T> context) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(DELAY_HEADER_NAME, delay);
        headers.put(Retry.RETRY_ATTEMPTS_HEADER_NAME, currentRetryAttempt + 1);

        logger.error(
                "Scheduling message type {} for retry on exception {}. Correlation ID: {} | Attempts: {} | Scheduled Time: {}",
                context.getTopic(),
                fault,
                context.getMessage().getCorrelationId(),
                currentRetryAttempt,
                Instant.now().toString()
        );

        String retryExchangeName = context.getTopic() + "_" + serviceName + "_Retry";
        bus.publish(true, retryExchangeName, headers, context.getBody());
    }

    private void sendToDeadLetterQueue(int retryCount, String topicName, UUID correlationId, Exception exception, Context<T> processingContext) {
        logger.error("Dead-lettering due to: {}", exception.getMessage(), exception);

        String deadLetterQueueName = processingContext.getTopic() + "_" + serviceName + "_Error";
        Map<String, Object> headers = createErrorHeaders(retryCount, topicName, correlationId, exception);
        bus.publish(false, deadLetterQueueName, headers, processingContext.getBody());
    }

    private Map<String, Object> createErrorHeaders(int retryCount, String topicName, UUID correlationId, Exception exception) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Service", serviceName);
        headers.put("TopicName", topicName);
        headers.put("RetryCount", retryCount);
        headers.put("Exception", exception.toString());
        headers.put("CorrelationId", correlationId.toString());
        headers.put("Timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        return headers;
    }
}