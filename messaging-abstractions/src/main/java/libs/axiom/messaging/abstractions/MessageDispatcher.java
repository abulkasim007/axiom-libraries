package libs.axiom.messaging.abstractions;

import java.util.Map;

public interface MessageDispatcher {

    String getTopic();

    String getTopicType();

    int getPrefetchSize();

    void dispatch(long messageId, boolean redeliver, byte[] message, Map<String, Object> headers);
}