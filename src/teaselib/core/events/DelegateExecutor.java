package teaselib.core.events;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

public class DelegateExecutor {
    private final ExecutorService workerThread;

    public interface Runnable {
        void run() throws Exception;
    }

    public DelegateExecutor(String name) {
        workerThread = NamedExecutorService.sameThread(name);
    }

    /**
     * Execute the delegate synchronized. The current thread waits until the delegates has completed execution.
     * 
     * @param delegate
     *            The delegate to execute in the delegate thread.
     */

    public void run(Runnable delegate) {
        try {
            Future<Void> future = workerThread.submit(() -> {
                delegate.run();
                return null;
            });
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        }
    }

    public <T> T call(Callable<T> delegate) {
        try {
            Future<T> future = workerThread.submit(delegate::call);
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
        workerThread.shutdown();
        try {
            workerThread.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
