package teaselib.core.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Signal {
    final Lock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    public static abstract class HasChangedPredicate
            implements Callable<Boolean> {
    }

    public void signal() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void doLocked(Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
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
            final HasChangedPredicate hasChangedPredicate)
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
                boolean inTime;
                long start = System.currentTimeMillis();
                long elapsed = 0;
                long timeoutMillis = (long) (timeoutSeconds) * 1000;
                do {
                    inTime = condition.await(timeoutMillis - elapsed,
                            TimeUnit.MILLISECONDS);
                    if (inTime) {
                        if (hasChangedPredicate.call()) {
                            break;
                        } else {
                            elapsed = System.currentTimeMillis() - start;
                        }
                    } else {
                        break;
                    }
                } while (timeoutMillis > elapsed);
                return inTime;
            }
        });
    }
}
