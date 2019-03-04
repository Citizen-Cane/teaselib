package teaselib.core.events;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class DelegateExecutor {
    private final ExecutorService workerThread;

    public DelegateExecutor(String name) {
        workerThread = NamedExecutorService.sameThread(name);
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     */

    // TODO Review all users and remove error handling where possible in order to handle errors here
    public void run(Runnable delegate) throws InterruptedException {
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

    public void shutdown() {
        workerThread.shutdownNow();
        try {
            workerThread.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
