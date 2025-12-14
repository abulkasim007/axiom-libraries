package libs.axiom.messaging.abstractions;

import java.util.Map;

public class Context<T extends Message> {
    private boolean isRetry;
    private boolean faulted;
    private boolean redeliver;
    private Exception fault;
    private boolean recoverableFault;
    private Map<String, Object> headers;

    private long id;
    private T message;
    private String topic;
    private byte[] body;
    private MessageHandler<T> messageHandler;

    public void setRedeliver(boolean redeliver) {
        this.redeliver = redeliver;
    }

    public static final class Retry {
        public static final String RETRY_ATTEMPTS_HEADER_NAME = "RetryAttempts";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public MessageHandler<T> getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;

        this.isRetry = headers != null && headers.containsKey(Retry.RETRY_ATTEMPTS_HEADER_NAME);
    }

    public boolean isFaulted() {
        return faulted;
    }

    public Exception getFault() {
        return this.fault;
    }

    public boolean isRecoverableFault() {
        return recoverableFault;
    }

    public boolean isRetry() {
        return isRetry;
    }

    public boolean isRedeliver() {
        return redeliver;
    }

    public void setFaulted(boolean recoverableFault, Exception fault) {
        this.fault = fault;
        this.faulted = true;
        this.recoverableFault = recoverableFault;
    }
}

