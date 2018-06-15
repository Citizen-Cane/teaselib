package teaselib.core.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Signal {
    final Lock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    @FunctionalInterface
    public interface HasChangedPredicate {
        boolean call();
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

    public boolean await(double timeoutSeconds) throws InterruptedException, Exception {
        return doLocked(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return condition.await((long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
        });
    }

    public boolean await(double timeoutSeconds, HasChangedPredicate hasChangedPredicate)
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
                long start = System.currentTimeMillis();
                long elapsed = 0;
                // TODO Double.MAX_Value overflow -> too much polling -> change to long + TimeUnit
                long timeoutMillis = timeoutSeconds == Double.MAX_VALUE ? Long.MAX_VALUE
                        : (long) (timeoutSeconds) * 1000;
                do {
                    boolean inTime = condition.await(timeoutMillis - elapsed, TimeUnit.MILLISECONDS);
                    if (inTime) {
                        if (hasChangedPredicate.call()) {
                            // Condition changed
                            return true;
                        } else {
                            // Something else changed
                            elapsed = System.currentTimeMillis() - start;
                        }
                    } else {
                        break;
                    }
                } while (timeoutMillis > elapsed);
                // condition may have become true since the last check
                return hasChangedPredicate.call();
            }
        });
    }
}
