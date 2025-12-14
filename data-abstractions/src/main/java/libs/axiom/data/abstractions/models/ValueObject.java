package libs.axiom.data.abstractions.models;

import java.util.UUID;

public class ValueObject implements Identity {

    private UUID id;

    public ValueObject(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
