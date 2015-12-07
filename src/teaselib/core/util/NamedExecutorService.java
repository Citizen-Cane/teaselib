/**
 * 
 */
package teaselib.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

// http://stackoverflow.com/questions/5740478/how-to-name-the-threads-of-a-thread-pool-in-java
/**
 * @author 
 *         http://stackoverflow.com/questions/5740478/how-to-name-the-threads-of-
 *         a-thread-pool-in-java
 *
 */
public class NamedExecutorService extends ThreadPoolExecutor {

    private static final String THREAD_NAME_PATTERN = "%s-%d";

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param namePrefix
     */
    public NamedExecutorService(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, final TimeUnit unit, final String namePrefix) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {

                    private final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        final String threadName = String.format(
                                THREAD_NAME_PATTERN, namePrefix,
                                counter.incrementAndGet());
                        return new Thread(r, threadName);
                    }
                });
    }

    public static ExecutorService newFixedThreadPool(int nThreads,
            String namePrefix, long keepAliveTime, final TimeUnit unit) {
        return new NamedExecutorService(0, nThreads, keepAliveTime, unit,
                namePrefix);
    }
}
