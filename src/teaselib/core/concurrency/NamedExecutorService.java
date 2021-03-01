package teaselib.core.concurrency;

import java.util.concurrent.BlockingQueue;
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

    private static final String MULTITHREADED_NAME_PATTERN = "%s-%d";
    private static final String SINGLETHREADED_NAME_PATTERN = "%s";

    private NamedExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, String name,
            BlockingQueue<Runnable> queue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, newThreadFactory(maximumPoolSize, name));
    }

    static ThreadFactory newThreadFactory(int maximumPoolSize, String name) {
        return maximumPoolSize == 1 ? r -> newSingleThread(name, r) : r -> newThread(name, r);
    }

    private static Thread newSingleThread(String name, Runnable r) {
        String threadName = String.format(SINGLETHREADED_NAME_PATTERN, name);
        return new Thread(r, threadName);
    }

    private static Thread newThread(String name, Runnable r) {
        AtomicInteger counter = new AtomicInteger();
        String threadName = String.format(MULTITHREADED_NAME_PATTERN, name, counter.incrementAndGet());
        return new Thread(r, threadName);
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
        return new NamedExecutorService(namePrefix, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public static NamedExecutorService singleThreadedQueue(String namePrefix, long keepAliveTime, TimeUnit unit) {
        return new NamedExecutorService(namePrefix, keepAliveTime, unit);
    }
}
