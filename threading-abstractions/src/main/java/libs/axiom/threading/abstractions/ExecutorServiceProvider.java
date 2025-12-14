package libs.axiom.threading.abstractions;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceProvider {
    ExecutorService getExecutorService();
}