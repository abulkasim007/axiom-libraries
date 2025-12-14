package libs.axiom.configuration.abstractions;

import java.util.UUID;

public record Vertical(
        UUID id,
        String name,
        Tenant[] tenants
) {
}
