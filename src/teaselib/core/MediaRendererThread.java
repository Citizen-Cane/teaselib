package teaselib.core;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import teaselib.TeaseLib;
import teaselib.core.util.NamedExecutorService;

/**
 * @author someone
 *
 */
public abstract class MediaRendererThread
        implements MediaRenderer.Threaded, MediaRenderer.Replay {
    protected final TeaseLib teaseLib;

    private final static ExecutorService Executor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE,
                    MediaRendererThread.class.getName() + " Render Task", 1,
                    TimeUnit.HOURS);

    protected Future<String> task = null;
    protected Position replayPosition = Position.FromStart;

    protected CountDownLatch completedStart = new CountDownLatch(1);
    protected CountDownLatch completedMandatory = new CountDownLatch(1);
    protected CountDownLatch completedAll = new CountDownLatch(1);

    private long startMillis = 0;

    public MediaRendererThread(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    @Override
    public final void render() {
        synchronized (this) {
            startMillis = System.currentTimeMillis();
            // renderThread.setName(getClass().getName());
            task = Executor.submit(new Callable<String>() {
                @Override
                public final String call() {
                    try {
                        synchronized (MediaRendererThread.this) {
                            MediaRendererThread.this.notifyAll();
                        }
                        renderMedia();
                    } catch (InterruptedException e) {
                        teaseLib.log.debug(this, e);
                    } catch (Throwable t) {
                        teaseLib.log.error(this, t);
                    }
                    startCompleted();
                    mandatoryCompleted();
                    allCompleted();
                    return null;
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
        teaseLib.log.info("Replay " + replayPosition.toString());
        render();
    }

    protected void startCompleted() {
        completedStart.countDown();
        teaseLib.log
        .debug(getClass().getSimpleName() + " completed start after "
                + String.format("%.2f seconds", getElapsedSeconds()));
    }

    protected void mandatoryCompleted() {
        completedMandatory.countDown();
        teaseLib.log.debug(
                getClass().getSimpleName() + " completed mandatory after "
                        + String.format("%.2f seconds", getElapsedSeconds()));
    }

    protected void allCompleted() {
        completedAll.countDown();
        teaseLib.log.debug(getClass().getSimpleName() + " completed all after "
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
        Future<String> f = task;
        if (f.isDone())
            return;
        try {
            completedAll.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
        try {
            f.get();
        } catch (CancellationException e) {
            throw new ScriptInterruptedException();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (ExecutionException e) {
            teaseLib.log.error(this, e);
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
        Future<String> f = task;
        if (!f.isCancelled()) {
            f.cancel(true);
        }
        teaseLib.log.debug(getClass().getSimpleName() + " cancelled after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    @Override
    public void join() {
        try {
            Future<String> f = task;
            f.get();
        } catch (CancellationException e) {
            // Expected
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (ExecutionException e) {
            teaseLib.log.error(this, e);
        }
        teaseLib.log.debug(getClass().getSimpleName() + " ended after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000;
    }
}