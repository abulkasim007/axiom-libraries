package libs.axiom.messaging.abstractions;

public final class ScopedContext {
    public final static ScopedValue<ThreadLocalContext> THREAD_LOCAL_CONTEXT = ScopedValue.newInstance();
}
