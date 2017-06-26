/**
 * 
 */
package teaselib.core.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author http://stackoverflow.com/questions/5740478/how-to-name-the-threads-
 *         of- a-thread-pool-in-java
 *
 */
public class NamedExecutorService extends ThreadPoolExecutor {

    private static final String MULTITHREADED_NAME_PATTERN = "%s-%d";
    private static final String SINGLETHREADED_NAME_PATTERN = "%s";

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param name
     */
    private NamedExecutorService(final int corePoolSize, final int maximumPoolSize, long keepAliveTime,
            final TimeUnit unit, final String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new SynchronousQueue<Runnable>(),
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

    private NamedExecutorService(final String name) {
        super(1, 1, 1, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                String namePattern = SINGLETHREADED_NAME_PATTERN;
                String threadName = String.format(namePattern, name);
                return new Thread(r, threadName);
            }
        });
    }

    public static ExecutorService newFixedThreadPool(int nThreads, String namePrefix, long keepAliveTime,
            final TimeUnit unit) {
        return new NamedExecutorService(0, nThreads, keepAliveTime, unit, namePrefix);
    }

    public static ExecutorService singleThreadedQueue(String namePrefix) {
        return new NamedExecutorService(namePrefix);
    }
}
