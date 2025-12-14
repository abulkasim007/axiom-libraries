package libs.axiom.messaging.abstractions.middlewares.resilience;

public class Retry {

    public static final String RETRY_ATTEMPTS_HEADER_NAME = "x-r-a";

    private String fault;
    private int delay;
    private int attempts;
    private boolean exponentialBackoff;

    public Retry(String fault, int delay, int attempts, boolean exponentialBackoff) {
        this.fault = fault;
        this.delay = delay;
        this.attempts = attempts;
        this.exponentialBackoff = exponentialBackoff;
    }

    public String getFault() {
        return fault;
    }

    public void setFault(String fault) {
        this.fault = fault;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isExponentialBackoff() {
        return exponentialBackoff;
    }

    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;
    }
}
