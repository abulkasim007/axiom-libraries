package libs.axiom.messaging.abstractions.middlewares.authorization;


import libs.axiom.messaging.abstractions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AuthorizationMiddleware<T extends Message> implements Middleware<T> {

    public static final UUID ANONYMOUS_ID = new UUID(0L, 0L);
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMiddleware.class);

    @Override
    public void invoke(Context<T> context, PipelineFunction<T> next) {
        if (context.isFaulted()) {
            next.apply(context);
            return;
        }

        UserContext userContext = context.getMessage().getUserContext();

        String error = validateUserContext(userContext);
        if (error != null) {
            logger.error(error);
            context.setFaulted(false, new Exception(error));
            next.apply(context);
            return;
        }

        error = validateMessage(context.getMessage());
        if (error != null) {
            logger.error(error);
            context.setFaulted(false, new Exception(error));
            next.apply(context);
            return;
        }

        next.apply(context);
    }

    private static String validateMessage(Message message) {
        if (message.getCorrelationId() == null || message.getCorrelationId().equals(ANONYMOUS_ID)) {
            return "Message authorization failure. Reason: No Correlation Id.";
        }
        return null;
    }

    // todo: the tenants, verticals etc should be tested against provided service registrations of current service.
    private static String validateUserContext(UserContext userContext) {
        if (userContext == null) {
            return "Message authorization failure. Reason: UserContext is null.";
        }
        if (userContext.getRoles() == null || userContext.getRoles().isEmpty()) {
            return "Message authorization failure. Reason: Roles are null or empty.";
        }
        if (userContext.getUserId() == null || userContext.getUserId().equals(ANONYMOUS_ID)) {
            return "Message authorization failure. Reason: UserId is invalid.";
        }
        if (userContext.getTenantId() == null || userContext.getTenantId().equals(ANONYMOUS_ID)) {
            return "Message authorization failure. Reason: TenantId is invalid.";
        }
        if (userContext.getVerticalId() == null || userContext.getVerticalId().equals(ANONYMOUS_ID)) {
            return "Message authorization failure. Reason: VerticalId is invalid.";
        }
        return null;
    }
}