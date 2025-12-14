package libs.axiom.data.abstractions.utils;

import java.util.Iterator;

public interface AutoCloseableIterator<T> extends Iterator<T>, AutoCloseable {
    @Override
    void close();
}