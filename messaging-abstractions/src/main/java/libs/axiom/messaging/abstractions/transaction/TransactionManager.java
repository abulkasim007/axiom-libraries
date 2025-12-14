package libs.axiom.messaging.abstractions.transaction;

import java.io.Closeable;

public interface TransactionManager extends Closeable {
    String name();

    void commit();

    void rollback();

    void close();
}