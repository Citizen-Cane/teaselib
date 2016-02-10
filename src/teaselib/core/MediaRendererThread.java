package teaselib.core;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import teaselib.TeaseLib;

/**
 * @author someone
 *
 */
public abstract class MediaRendererThread
        implements Runnable, MediaRenderer.Threaded, MediaRenderer.Replay {
    protected final TeaseLib teaseLib;

    protected Thread renderThread = null;
    protected boolean endThread = false;
    protected Position replayPosition = Position.FromStart;

    protected CountDownLatch completedStart = new CountDownLatch(1);
    protected CountDownLatch completedMandatory = new CountDownLatch(1);
    protected CountDownLatch completedAll = new CountDownLatch(1);

    private long start = 0;

    public MediaRendererThread(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    /**
     * The render method executed by the render thread
     */
    protected abstract void renderMedia()
            throws InterruptedException, IOException;

    @Override
    public final void run() {
        try {
            synchronized (renderThread) {
                renderThread.notifyAll();
            }
            renderMedia();
        } catch (InterruptedException e) {
            teaseLib.log.debug(this, e);
        } catch (Throwable t) {
            teaseLib.log.error(this, t);
        }
        endThread = true;
        startCompleted();
        mandatoryCompleted();
        allCompleted();
    }

    @Override
    public final void render() {
        endThread = false;
        renderThread = new Thread(this);
        synchronized (renderThread) {
            start = System.currentTimeMillis();
            renderThread.setName(getClass().getName());
            renderThread.start();
            try {
                renderThread.wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

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
    }

    protected void mandatoryCompleted() {
        completedMandatory.countDown();
    }

    protected void allCompleted() {
        completedAll.countDown();
    }

    @Override
    public void completeStart() {
        if (!endThread) {
            try {
                completedStart.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        teaseLib.log
                .debug(getClass().getSimpleName() + " completed start after "
                        + String.format("%.2f seconds", getElapsedSeconds()));
    }

    @Override
    public void completeMandatory() {
        if (!endThread) {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        teaseLib.log.debug(
                getClass().getSimpleName() + " completed mandatory after "
                        + String.format("%.2f seconds", getElapsedSeconds()));
    }

    @Override
    public void completeAll() {
        Thread thread = renderThread;
        if (!thread.isAlive())
            return;
        if (!endThread) {
            try {
                completedAll.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        while (thread.isAlive()) {
            try {
                thread.join();
                teaseLib.log.debug(
                        getClass().getSimpleName() + " completed all after "
                                + String.format("%.2f", getElapsedSeconds()));
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
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
        Thread thread = renderThread;
        if (!thread.isAlive())
            endThread = true;
        thread.interrupt();
        teaseLib.log.debug(getClass().getSimpleName() + " interrupted after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    @Override
    public void join() {
        // Almost like complete, but avoid recursion
        Thread thread = renderThread;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        teaseLib.log.debug(getClass().getSimpleName() + " ended after "
                + String.format("%.2f", getElapsedSeconds()));
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - start) / 1000;
    }
}