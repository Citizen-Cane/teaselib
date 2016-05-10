package teaselib.core.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Signal {
    final Lock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    public void signal() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean doLocked(Callable<Boolean> callable) throws Exception {
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    public boolean await(final double timeoutSeconds)
            throws InterruptedException, Exception {
        return doLocked(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return condition.await((long) timeoutSeconds * 1000,
                        TimeUnit.MILLISECONDS);
            }
        });
    }

    public boolean awaitChange(final double timeoutSeconds,
            final Callable<Boolean> hasChangedPredicate)
                    throws InterruptedException, Exception {
        return doLocked(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean changed = hasChangedPredicate.call();
                if (!changed) {
                    changed = poll();
                }
                return changed;
            }

            private boolean poll() throws InterruptedException, Exception {
                boolean changed;
                long start = System.currentTimeMillis();
                long elapsed = 0;
                long timeoutMillis = (long) (timeoutSeconds) * 1000;
                do {
                    changed = condition.await(timeoutMillis - elapsed,
                            TimeUnit.MILLISECONDS);
                    if (changed && !hasChangedPredicate.call()) {
                        elapsed = System.currentTimeMillis() - start;
                        continue;
                    }
                } while (timeoutMillis > elapsed);
                return changed;
            }
        });
    }
}
