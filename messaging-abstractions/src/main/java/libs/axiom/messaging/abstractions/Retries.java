package libs.axiom.messaging.abstractions;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import libs.axiom.messaging.abstractions.middlewares.resilience.Retry;

public class Retries {
    private final Multibinder<Retry> multibinder;

    private Retries(Binder binder) {

        this.multibinder = Multibinder.newSetBinder(binder, Retry.class);
    }

    public static Retries from(Binder binder) {
        return new Retries(binder);
    }

    public <T extends Exception> Retries add(Class<T> exceptionType, int delay, int attempts, boolean exponentialBackoff) {

        if (delay <= 0) {
            throw new IllegalArgumentException("delay must be greater than 0");
        }

        if (attempts <= 0) {
            throw new IllegalArgumentException("attempts must be greater than 0");
        }

        multibinder.addBinding().toInstance(new Retry(exceptionType.getTypeName(), delay, attempts, exponentialBackoff));
        return this;
    }
}
