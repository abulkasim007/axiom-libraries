package libs.axiom.messaging.abstractions;

import java.util.ArrayList;
import java.util.List;

public class ThreadLocalContext {

    private final Message message;

    private final List<FeatureItem> entries = new ArrayList<>();

    public ThreadLocalContext(final Message message) {
        this.message = message;
    }

    public void setFeature(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }

        Class<?> type = value.getClass();

        entries.removeIf(item -> item.type().equals(type));
        entries.add(new FeatureItem(type, value));
    }

    @SuppressWarnings("unchecked")
    public <T> T getFeature(Class<T> type) {
        for (var entry : entries) {
            if (entry.type().equals(type)) {
                return (T) entry.value();
            }
        }
        return null;
    }



    @SuppressWarnings("unchecked")
    public <T> T getFeatureOfBaseType(Class<T> baseType) {
        if (baseType == null) {
            throw new IllegalArgumentException("baseType is null");
        }

        for (var entry : entries) {
            if (baseType.isAssignableFrom(entry.type())) {
               return (T) entry.value();
            }
        }

        return null;
    }

    public Message getMessage() {
        return message;
    }

    private record FeatureItem(Class<?> type, Object value) {
    }
}