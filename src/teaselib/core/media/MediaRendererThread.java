package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author someone
 *
 */
public abstract class MediaRendererThread
        implements MediaRenderer.Threaded, MediaRenderer.Replay {
    private static final Logger logger = LoggerFactory
            .getLogger(MediaRendererThread.class);
    protected final TeaseLib teaseLib;

    private final static String RenderTaskBaseName = "RenderTask ";
    private final static ExecutorService Executor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE, RenderTaskBaseName, 1,
                    TimeUnit.HOURS);

    protected Future<?> task = null;
    protected Position replayPosition = Position.FromStart;

    protected CountDownLatch completedStart = new CountDownLatch(1);
    protected CountDownLatch completedMandatory = new CountDownLatch(1);
    protected CountDownLatch completedAll = new CountDownLatch(1);

    private long startMillis = 0;

    public MediaRendererThread(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    private String nameForActiveThread() {
        return this.getClass().getSimpleName();
    }

    private static String nameForSleepingThread() {
        return RenderTaskBaseName + "pool thread";
    }

    private static void setThreadName(String name) {
        Thread.currentThread().setName(name);
    }

    @Override
    public final void render() {
        synchronized (this) {
            startMillis = System.currentTimeMillis();
            task = Executor.submit(new Runnable() {
                @Override
                public void run() {
                    setThreadName(nameForActiveThread());
                    try {
                        synchronized (MediaRendererThread.this) {
                            MediaRendererThread.this.notifyAll();
                        }
                        renderMedia();
                    } catch (InterruptedException e) {
                        // Expected
                    } catch (ScriptInterruptedException e) {
                        // Expected
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                    } finally {
                        startCompleted();
                        mandatoryCompleted();
                        allCompleted();
                        setThreadName(nameForSleepingThread());
                    }
                }
            });
            try {
                // Wait until the renderer has started
                wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    /**
     * The render method executed by the render thread
     */
    protected abstract void renderMedia()
            throws InterruptedException, IOException;

    @Override
    public void replay(Position replayPosition) {
        this.replayPosition = replayPosition;
        if (replayPosition == Position.FromStart) {
            // Skip
        } else if (replayPosition == Position.FromStart) {
            completedStart = new CountDownLatch(1);
        } else {
            completedStart = new CountDownLatch(1);
            completedMandatory = new CountDownLatch(1);
        }
        logger.info("Replay " + replayPosition.toString());
        render();
    }

    protected void startCompleted() {
        completedStart.countDown();
        logger.debug(getClass().getSimpleName() + " completed start after "
                + String.format("%.2f seconds", getElapsedSeconds()));
    }

    protected void mandatoryCompleted() {
        completedMandatory.countDown();
        logger.debug(getClass().getSimpleName() + " completed mandatory after "
                + String.format("%.2f seconds", getElapsedSeconds()));
    }

    protected void allCompleted() {
        completedAll.countDown();
        logger.debug(getClass().getSimpleName() + " completed all after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    @Override
    public void completeStart() {
        if (!task.isDone()) {
            try {
                completedStart.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    @Override
    public void completeMandatory() {
        if (!task.isDone()) {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    @Override
    public void completeAll() {
        Future<?> f = task;
        if (f.isDone())
            return;
        try {
            completedAll.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public boolean hasCompletedStart() {
        return completedStart.getCount() == 0;
    }

    @Override
    public boolean hasCompletedMandatory() {
        return completedMandatory.getCount() == 0;
    }

    @Override
    public boolean hasCompletedAll() {
        return completedAll.getCount() == 0;
    }

    @Override
    public void interrupt() {
        Future<?> f = task;
        if (!f.isCancelled()) {
            f.cancel(true);
        }
        logger.debug(getClass().getSimpleName() + " cancelled after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    @Override
    public void join() {
        try {
            Future<?> f = task;
            f.get();
        } catch (CancellationException e) {
            // Expected
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
        logger.debug(getClass().getSimpleName() + " ended after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000;
    }
}