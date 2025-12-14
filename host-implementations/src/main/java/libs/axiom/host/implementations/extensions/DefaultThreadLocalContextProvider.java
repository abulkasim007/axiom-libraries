package libs.axiom.host.implementations.extensions;

import libs.axiom.messaging.abstractions.ScopedContext;
import libs.axiom.messaging.abstractions.ThreadLocalContext;
import libs.axiom.messaging.abstractions.ThreadLocalContextProvider;

public class DefaultThreadLocalContextProvider implements ThreadLocalContextProvider {

    @Override
    public ThreadLocalContext getThreadLocalContext() {
        return ScopedContext.THREAD_LOCAL_CONTEXT.get();
    }
}
