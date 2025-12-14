package libs.axiom.messaging.abstractions;

public class MalformedPayloadException extends Exception {
    public MalformedPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}