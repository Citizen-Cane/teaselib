package teaselib.core.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractFuture<T> implements Future<T> {

    protected final Future<T> future;

    @Override
    public boolean cancel(boolean interrupt) {
        return future.cancel(interrupt);
    }

    public AbstractFuture(Future<T> future) {
        this.future = future;
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

}