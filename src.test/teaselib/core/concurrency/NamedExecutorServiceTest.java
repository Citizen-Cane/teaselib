package teaselib.core.concurrency;


import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamedExecutorServiceTest {

    private final CountDownLatch complete = new CountDownLatch(1);

    private final Runnable test = () -> {
        try {
            synchronized (NamedExecutorServiceTest.this) {
                NamedExecutorServiceTest.this.notifyAll();
            }
            complete.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    @Test
    public void testNewUnlimitedThreadPool() throws InterruptedException {
        NamedExecutorService unlimitedThreadPool = NamedExecutorService.newUnlimitedThreadPool("test", Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);

        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }
        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }

        assertEquals(2, unlimitedThreadPool.getActiveCount());
        assertEquals(2, unlimitedThreadPool.getLargestPoolSize());

        unlimitedThreadPool.submit(test);
        assertEquals(3, unlimitedThreadPool.getActiveCount());
        assertEquals(3, unlimitedThreadPool.getLargestPoolSize());

        complete.countDown();

        unlimitedThreadPool.shutdown();
        unlimitedThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        assertEquals(0, unlimitedThreadPool.getActiveCount());
        assertEquals(3, unlimitedThreadPool.getLargestPoolSize());
    }

    @Test
    public void testNewFixedThreadPool() throws Exception {
        NamedExecutorService unlimitedThreadPool = NamedExecutorService.newFixedThreadPool(2, "test", Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);

        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }
        assertEquals(1, unlimitedThreadPool.getActiveCount());

        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }
        assertEquals(2, unlimitedThreadPool.getActiveCount());

        unlimitedThreadPool.submit(test);
        assertEquals(1, unlimitedThreadPool.getQueue().size());
        assertEquals(2, unlimitedThreadPool.getActiveCount());

        assertEquals(2, unlimitedThreadPool.getLargestPoolSize());

        complete.countDown();

        unlimitedThreadPool.shutdown();
        unlimitedThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        assertEquals(0, unlimitedThreadPool.getActiveCount());
        assertEquals(2, unlimitedThreadPool.getLargestPoolSize());
    }

    @Test
    public void testNewFixedThreadPoolWithOneThread() throws Exception {
        NamedExecutorService unlimitedThreadPool = NamedExecutorService.newFixedThreadPool(2, "test", Long.MAX_VALUE,
                TimeUnit.MILLISECONDS);

        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }
        assertEquals(1, unlimitedThreadPool.getActiveCount());

        complete.countDown();

        unlimitedThreadPool.shutdown();
        unlimitedThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        assertEquals(0, unlimitedThreadPool.getActiveCount());
        assertEquals(1, unlimitedThreadPool.getLargestPoolSize());
    }

    @Test
    public void testSingleThreadedQueue() throws Exception {
        NamedExecutorService unlimitedThreadPool = NamedExecutorService.singleThreadedQueue("test");

        synchronized (this) {
            unlimitedThreadPool.submit(test);
            this.wait();
        }
        assertEquals(1, unlimitedThreadPool.getActiveCount());

        unlimitedThreadPool.submit(test);
        assertEquals(1, unlimitedThreadPool.getActiveCount());

        unlimitedThreadPool.submit(test);
        assertEquals(2, unlimitedThreadPool.getQueue().size());
        assertEquals(1, unlimitedThreadPool.getActiveCount());

        assertEquals(1, unlimitedThreadPool.getLargestPoolSize());

        complete.countDown();

        unlimitedThreadPool.shutdown();
        unlimitedThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        assertEquals(0, unlimitedThreadPool.getActiveCount());
        assertEquals(1, unlimitedThreadPool.getLargestPoolSize());
    }
}
