package teaselib.userinterface;

import java.util.concurrent.CountDownLatch;

import teaselib.ScriptInterruptedException;
import teaselib.TeaseLib;

public abstract class MediaRendererThread implements Runnable, MediaRenderer,
        MediaRenderer.Threaded {

    protected Thread renderThread = null;
    protected boolean endThread = false;
    protected TeaseLib teaseLib = null;

    protected final CountDownLatch completedStart = new CountDownLatch(1);
    protected final CountDownLatch completedMandatory = new CountDownLatch(1);
    protected final CountDownLatch completedAll = new CountDownLatch(1);

    private long start = 0;

    /**
     * The render method executed by the render thread
     */
    protected abstract void render() throws InterruptedException;

    @Override
    public final void run() {
        try {
            synchronized (renderThread) {
                renderThread.notifyAll();
            }
            render();
        } catch (InterruptedException e) {
            TeaseLib.logDetail(this, e);
        } catch (Throwable t) {
            TeaseLib.log(this, t);
        }
        endThread = true;
        startCompleted();
        mandatoryCompleted();
        allCompleted();
    }

    @Override
    public void render(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
        endThread = false;
        renderThread = new Thread(this);
        synchronized (renderThread) {
            start = System.currentTimeMillis();
            renderThread.start();
            try {
                renderThread.wait();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
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

    public void completeStart() {
        if (!endThread) {
            try {
                completedStart.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        TeaseLib.logDetail(getClass().getSimpleName()
                + " completed start after "
                + String.format("%.2f seconds", getElapsedSeconds()));
    }

    public void completeMandatory() {
        if (!endThread) {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        TeaseLib.logDetail(getClass().getSimpleName()
                + " completed mandatory after "
                + String.format("%.2f seconds", getElapsedSeconds()));
    }

    public void completeAll() {
        if (renderThread == null)
            return;
        if (!endThread) {
            try {
                completedMandatory.await();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
        try {
            while (renderThread.isAlive()) {
                try {
                    renderThread.join();
                    TeaseLib.logDetail(getClass().getSimpleName()
                            + " completed all after "
                            + String.format("%.2f", getElapsedSeconds()));
                } catch (InterruptedException e) {
                    end();
                    throw new ScriptInterruptedException();
                }
            }
        } finally {
            renderThread = null;
        }
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - start) / 1000;
    }

    @Override
    public void end() {
        if (renderThread == null)
            return;
        endThread = true;
        renderThread.interrupt();
        TeaseLib.logDetail(getClass().getSimpleName() + " interrupted after "
                + String.format("%.2f", getElapsedSeconds()));
        // Almost like complete, but avoid recursion
        try {
            while (renderThread.isAlive()) {
                try {
                    renderThread.join();
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        } finally {
            renderThread = null;
            TeaseLib.logDetail(getClass().getSimpleName() + " ended after "
                    + String.format("%.2f", getElapsedSeconds()));
        }
    }
}