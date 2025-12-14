package libs.axiom.host.implementations.extensions;

import libs.axiom.messaging.abstractions.Message;
import libs.axiom.messaging.abstractions.MessageProvider;
import libs.axiom.messaging.abstractions.ScopedContext;

public class DefaultMessageProvider implements MessageProvider {

    public Message getMessage() {
        return ScopedContext.THREAD_LOCAL_CONTEXT.get().getMessage();
    }
}
