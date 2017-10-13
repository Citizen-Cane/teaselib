package teaselib.core.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class DelegateThread {
    private final ExecutorService workerThread;

    public DelegateThread(String name) {
        workerThread = NamedExecutorService.singleThreadedQueue(name);
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     * @throws Throwable
     *             If the delegate throws, the throwable is forwarded to the current thread.
     */
    public void run(Delegate delegate) throws InterruptedException {
        try {
            Future<?> future = workerThread.submit(() -> delegate.run());
            future.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }
}
