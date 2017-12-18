package teaselib.core.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
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

    private NamedExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        boolean multiThreaded = maximumPoolSize > 1;
                        String namePattern = multiThreaded ? MULTITHREADED_NAME_PATTERN : SINGLETHREADED_NAME_PATTERN;
                        String threadName = String.format(namePattern, name, counter.incrementAndGet());
                        return new Thread(r, threadName);
                    }
                });
    }

    private NamedExecutorService(String name, long keepAliveTime, TimeUnit unit) {
        super(1, 1, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), (ThreadFactory) r -> {
            String namePattern = SINGLETHREADED_NAME_PATTERN;
            String threadName = String.format(namePattern, name);
            return new Thread(r, threadName);
        });
    }

    public static ExecutorService newFixedThreadPool(int nThreads, String namePrefix, long keepAliveTime,
            TimeUnit unit) {
        return new NamedExecutorService(0, nThreads, keepAliveTime, unit, namePrefix);
    }

    public static ExecutorService singleThreadedQueue(String namePrefix) {
        return new NamedExecutorService(namePrefix, Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public static ExecutorService singleThreadedQueue(String namePrefix, long keepAliveTime, TimeUnit unit) {
        return new NamedExecutorService(namePrefix, keepAliveTime, unit);
    }
}
