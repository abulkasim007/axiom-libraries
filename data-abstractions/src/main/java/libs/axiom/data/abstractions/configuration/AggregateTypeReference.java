package libs.axiom.data.abstractions.configuration;

import libs.axiom.data.abstractions.models.AggregateRoot;

public class AggregateTypeReference<T extends AggregateRoot> {

    private final Class<T> type;

    public AggregateTypeReference(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }
}
