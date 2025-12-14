package libs.axiom.data.abstractions.exceptions;

import java.util.UUID;

public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException(UUID aggregateRootId, int expectedVersion, int actualVersion) {
        super(buildErrorMessage(aggregateRootId, expectedVersion, actualVersion));
    }

    private static String buildErrorMessage(UUID aggregateRootId, int expectedVersion, int actualVersion) {
        return String.format(
                "Concurrency Exception | AggregateRootId: %s | Expected version: %d | Actual version: %d",
                aggregateRootId,
                expectedVersion,
                actualVersion
        );
    }
}
