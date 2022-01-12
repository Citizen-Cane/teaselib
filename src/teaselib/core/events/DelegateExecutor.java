package teaselib.core.events;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class DelegateExecutor {

    private final ExecutorService executor;

    public interface Runnable {
        void run() throws Exception;
    }

    public DelegateExecutor(String name) {
        executor = NamedExecutorService.sameThread(name);
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegate has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     */
    public void run(Runnable delegate) {
        call(() -> {
            delegate.run();
            return null;
        });
    }

    public <T> T call(Callable<T> delegate) {
        try {
            Future<T> future = executor.submit(delegate::call);
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
