package teaselib.core.events;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class DelegateExecutor {
    private final ExecutorService workerThread;

    public DelegateExecutor(String name) {
        workerThread = NamedExecutorService.singleThreadedQueue(name);
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     */
    public void run(Delegate delegate) throws InterruptedException {
        try {
            Future<?> future = workerThread.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    delegate.run();
                    return null;
                }
            });

            future.get();
        } catch (RuntimeException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }
}