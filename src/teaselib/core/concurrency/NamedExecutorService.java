package teaselib.core.concurrency;

import static teaselib.core.util.ExceptionUtil.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Citizen-Cane
 *
 */
public class NamedExecutorService extends ThreadPoolExecutor {

    private static final String MULTITHREADED_NAME_PATTERN = "%s %d";
    private static final String SINGLETHREADED_NAME_PATTERN = "%s";

    private NamedExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, String name,
            BlockingQueue<Runnable> queue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, newThreadFactory(maximumPoolSize, name));
    }

    static ThreadFactory newThreadFactory(int maximumPoolSize, String name) {
        if (maximumPoolSize == 1) {
            return r -> newSingleThread(name, r);
        } else {
            AtomicInteger counter = new AtomicInteger();
            return r -> newThread(name, r, counter.incrementAndGet());
        }
    }

    private static Thread newSingleThread(String name, Runnable r) {
        String threadName = String.format(SINGLETHREADED_NAME_PATTERN, name);
        Thread thread = new Thread(r, threadName);
        thread.setUncaughtExceptionHandler(UncaughtExceptionHandler.Instance);
        return thread;
    }

    private static Thread newThread(String name, Runnable r, int n) {
        String threadName = String.format(MULTITHREADED_NAME_PATTERN, name, n);
        Thread thread = new Thread(r, threadName);
        thread.setUncaughtExceptionHandler(UncaughtExceptionHandler.Instance);
        return thread;
    }

    private NamedExecutorService(String name, long keepAliveTime, TimeUnit unit) {
        this(name, keepAliveTime, unit, new LinkedBlockingQueue<>());
    }

    private NamedExecutorService(String name, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> queue) {
        this(1, 1, keepAliveTime, unit, name, queue);
    }

    public static NamedExecutorService newUnlimitedThreadPool(String namePrefix, long keepAliveTime, TimeUnit unit) {
        return new NamedExecutorService(0, Integer.MAX_VALUE, keepAliveTime, unit, namePrefix,
                new SynchronousQueue<>());
    }

    public static NamedExecutorService newFixedThreadPool(int nThreads, String namePrefix) {
        return newFixedThreadPool(nThreads, namePrefix, 1L, TimeUnit.MINUTES);
    }

    public static NamedExecutorService newFixedThreadPool(int nThreads, String namePrefix, long keepAliveTime,
            TimeUnit unit) {
        return new NamedExecutorService(nThreads, nThreads, keepAliveTime, unit, namePrefix,
                new LinkedBlockingQueue<>());
    }

    public static NamedExecutorService singleThreadedQueue(String namePrefix) {
        return new NamedExecutorService(namePrefix, 1, TimeUnit.MINUTES);
    }

    public static NamedExecutorService sameThread(String namePrefix) {
        return new SameThread(namePrefix);
    }

    public static class SameThread extends NamedExecutorService {
        public SameThread(String namePrefix) {
            super(namePrefix, Long.MAX_VALUE, TimeUnit.SECONDS);
        }

    }

    public static NamedExecutorService singleThreadedQueue(String namePrefix, long keepAliveTime, TimeUnit unit) {
        return new NamedExecutorService(namePrefix, keepAliveTime, unit);
    }

    public <T> T submitAndGet(Callable<T> task) throws InterruptedException {
        try {
            return submit(task).get();
        } catch (ExecutionException e) {
            throw asRuntimeException(reduce(e));
        }
    }

}
