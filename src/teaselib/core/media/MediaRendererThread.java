package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Config;
import teaselib.Replay;
import teaselib.core.Configuration;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.util.ExceptionUtil;

/**
 * @author someone
 *
 */
public abstract class MediaRendererThread implements MediaRenderer.Threaded, ReplayableMediaRenderer {
    private static final Logger logger = LoggerFactory.getLogger(MediaRendererThread.class);
    protected final TeaseLib teaseLib;

    private final static String RenderTaskBaseName = "RenderTask ";
    private final static ExecutorService Executor = NamedExecutorService.newFixedThreadPool(Integer.MAX_VALUE,
            RenderTaskBaseName, 1, TimeUnit.HOURS);

    protected Future<?> task = null;
    protected Replay.Position replayPosition = Replay.Position.FromStart;

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
            task = Executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
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
                    } catch (Exception e) {
                        handleException(ExceptionUtil.reduce(e));
                    } finally {
                        startCompleted();
                        mandatoryCompleted();
                        allCompleted();
                        setThreadName(nameForSleepingThread());
                    }
                    return null;
                }

                private void handleException(Exception e) throws Exception {
                    if (e instanceof IOException) {
                        handleIOException(e);
                    } else {
                        boolean stopOnRenderError = Boolean
                                .parseBoolean(teaseLib.config.get(Config.Debug.StopOnRenderError));
                        if (stopOnRenderError) {
                            // TODO Error is not (always) forwarded to script thread
                            logger.error(e.getMessage(), e);
                            throw e;
                        } else {
                            logger.warn(e.getMessage(), e);
                        }
                    }
                }
            });
            try {
                // Wait until the renderer has started
                wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException(e);
            }
        }
    }

    protected void handleIOException(Exception e) throws IOException {
        handleIOException(teaseLib.config, e);
    }

    public static void handleIOException(Configuration config, Exception e) throws IOException {
        if (e instanceof IOException) {
            boolean stopOnAssetNotFound = Boolean.parseBoolean(config.get(Config.Debug.StopOnAssetNotFound));
            if (stopOnAssetNotFound) {
                logger.error(e.getMessage(), e);
                throw (IOException) e;
            } else {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * The render method executed by the render thread
     */
    protected abstract void renderMedia() throws InterruptedException, IOException;

    @Override
    public void replay(Replay.Position replayPosition) {
        this.replayPosition = replayPosition;
        if (replayPosition == Replay.Position.FromStart) {
            // Skip
        } else if (replayPosition == Replay.Position.FromStart) {
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
        logger.debug(getClass().getSimpleName() + " completed all after " + String.format("%.2f", getElapsedSeconds()));
    }

    @Override
    public void completeStart() {
        if (!isDoneOrCancelled()) {
            try {
                completedStart.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException(e);
            }
        }
    }

    @Override
    public void completeMandatory() {
        if (!isDoneOrCancelled()) {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException(e);
            }
        }
    }

    @Override
    public void completeAll() {
        if (!isDoneOrCancelled()) {
            try {
                completedAll.await();
                join();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException(e);
            }
        }
    }

    private boolean isDoneOrCancelled() {
        Future<?> f = task;
        return f.isDone() || f.isCancelled();
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
            logger.debug(getClass().getSimpleName() + " cancelled after " + String.format("%.2f", getElapsedSeconds()));
        }
    }

    @Override
    public void join() {
        try {
            Future<?> f = task;
            f.get();
        } catch (CancellationException e) {
            // Expected
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } catch (Exception e) {
            Exception cause = ExceptionUtil.reduce(e);
            throw ExceptionUtil.asRuntimeException(cause);
        } finally {
            logger.debug(getClass().getSimpleName() + " ended after " + String.format("%.2f", getElapsedSeconds()));
        }
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000;
    }
}