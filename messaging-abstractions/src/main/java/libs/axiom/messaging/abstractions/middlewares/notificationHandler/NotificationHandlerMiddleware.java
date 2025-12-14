package libs.axiom.messaging.abstractions.middlewares.notificationHandler;

import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.*;

import java.util.*;

public class NotificationHandlerMiddleware<T extends Message> implements Middleware<T> {

    public static final String BACK_END_ERROR_TOPIC_NAME = "SomethingWentWrong";

    private final Bus bus;

    @Inject
    public NotificationHandlerMiddleware(Bus bus) {
        this.bus = bus;
    }

    @Override
    public void invoke(Context<T> context, PipelineFunction<T> next) {
        if (context.isFaulted()) {
            if (context.isRecoverableFault()) {
                NotifyCommand notifyCommand = getNotifyCommand(context.getMessage(), context.getTopic());
                bus.send(notifyCommand);
                return;
            }
        }

        if (context.getMessage() instanceof NotifiedByEventHandler notificationMessage) {
            NotifyCommand notifyCommand = getNotifyCommand(notificationMessage, context.getMessage());
            bus.send(notifyCommand);
            return;
        }

        next.apply(context);
    }

    private static NotifyCommand getNotifyCommand(NotifiedByEventHandler notificationMessage, Message message) {
        NotifyCommand command = new NotifyCommand();
        command.setCorrelationId(message.getCorrelationId());
        command.setDevices(new ArrayList<>(notificationMessage.getDevices()));
        command.setTopic(notificationMessage.getTopic());
        command.setMessage(new HashMap<>(notificationMessage.getMessage()));
        command.setOffline(notificationMessage.isOffline());
        command.setRoles(new ArrayList<>(notificationMessage.getRoles()));
        command.setUserContext(message.getUserContext());
        command.setUserIds(new ArrayList<>(notificationMessage.getUserIds()));
        command.setSessionIds(new ArrayList<>(notificationMessage.getSessionIds()));
        return command;
    }

    private static NotifyCommand getNotifyCommand(Message message, String messageType) {
        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("Code", 500);
        errorMessage.put("Type", "Error");
        errorMessage.put("MessageType", messageType);

        List<Devices> devices = List.of(Devices.WEB);

        UserContext userContext = message.getUserContext();
        List<UUID> userIds = new ArrayList<>();
        List<UUID> sessionIds = new ArrayList<>();

        UUID anonymousUserId = UserContext.ANONYMOUS_USER_ID;

        if (!userContext.getUserId().equals(anonymousUserId)) {
            userIds.add(userContext.getUserId());
            sessionIds.add(userContext.getSessionId());
        }

        NotifyCommand command = new NotifyCommand();
        command.setCorrelationId(message.getCorrelationId());
        command.setDevices(devices);
        command.setMessage(errorMessage);
        command.setRoles(new ArrayList<>(userContext.getRoles()));
        command.setUserContext(userContext);
        command.setTopic(BACK_END_ERROR_TOPIC_NAME);
        command.setUserIds(userIds);
        command.setSessionIds(sessionIds);

        return command;
    }
}
