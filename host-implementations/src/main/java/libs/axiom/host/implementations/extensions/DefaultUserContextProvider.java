package libs.axiom.host.implementations.extensions;

import jakarta.inject.Inject;
import libs.axiom.messaging.abstractions.MessageProvider;
import libs.axiom.messaging.abstractions.UserContext;
import libs.axiom.messaging.abstractions.UserContextProvider;

public class DefaultUserContextProvider implements UserContextProvider {

    private final MessageProvider messageProvider;

    @Inject
    public DefaultUserContextProvider(MessageProvider messageProvider) {

        this.messageProvider = messageProvider;
    }

    public UserContext getUserContext() {
        return messageProvider.getMessage().getUserContext();
    }
}