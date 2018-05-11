package teaselib.core.media;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Replay;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseLib;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class MediaRendererThread implements MediaRenderer.Threaded {
    private static final Logger logger = LoggerFactory.getLogger(MediaRendererThread.class);

    protected final TeaseLib teaseLib;
    protected Replay.Position replayPosition = Replay.Position.FromStart;

    protected CountDownLatch completedStart = new CountDownLatch(1);
    protected CountDownLatch completedMandatory = new CountDownLatch(1);
    protected CountDownLatch completedAll = new CountDownLatch(1);

    private long startMillis = 0;

    public MediaRendererThread(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    @Override
    public final void run() {
        startMillis = System.currentTimeMillis();
        try {
            renderMedia();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Expected
        } catch (ScriptInterruptedException e) {
            // Expected
        } catch (Exception e) {
            ExceptionUtil.handleException(e, teaseLib.config, logger);
        } finally {
            startCompleted();
            mandatoryCompleted();
            allCompleted();
        }
    }

    protected void handleIOException(IOException e) throws IOException {
        ExceptionUtil.handleIOException(e, teaseLib.config, logger);
    }

    /**
     * The render method executed by the render thread
     */
    protected abstract void renderMedia() throws InterruptedException, IOException;

    public void replay(Replay.Position replayPosition) {
        this.replayPosition = replayPosition;
        if (replayPosition == Replay.Position.FromStart) {
            completedStart = new CountDownLatch(1);
            completedMandatory = new CountDownLatch(1);
            completedAll = new CountDownLatch(1);
        } else if (replayPosition == Replay.Position.FromCurrentPosition) {
            completedStart = new CountDownLatch(0);
        } else if (replayPosition == Replay.Position.FromMandatory) {
            completedStart = new CountDownLatch(0);
            completedMandatory = new CountDownLatch(1);
            completedAll = new CountDownLatch(1);
        } else if (replayPosition == Replay.Position.End) {
            completedStart = new CountDownLatch(0);
            completedMandatory = new CountDownLatch(0);
            completedAll = new CountDownLatch(1);
        } else {
            throw new IllegalArgumentException(replayPosition.toString());
        }
        logger.info("Replay {}", replayPosition);
    }

    protected final void startCompleted() {
        completedStart.countDown();
        if (logger.isDebugEnabled()) {
            logger.debug(getClass().getSimpleName() + " completed start after "
                    + String.format("%.2f seconds", getElapsedSeconds()));
        }
    }

    protected final void mandatoryCompleted() {
        completedMandatory.countDown();
        if (logger.isDebugEnabled()) {
            logger.debug("{} completed mandatory after {}", getClass().getSimpleName(),
                    String.format("%.2f seconds", getElapsedSeconds()));
        }
    }

    protected final void allCompleted() {
        completedAll.countDown();
        if (logger.isDebugEnabled()) {
            logger.debug("{} completed all after {}", getClass().getSimpleName(), getElapsedSecondsFormatted());
        }
    }

    @Override
    public void completeStart() {
        try {
            completedStart.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    @Override
    public void completeMandatory() {
        try {
            completedMandatory.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    @Override
    public void completeAll() {
        try {
            completedAll.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
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

    public String getElapsedSecondsFormatted() {
        return String.format("%.2f", getElapsedSeconds());
    }

    private double getElapsedSeconds() {
        return (System.currentTimeMillis() - startMillis) / 1000.0;
    }
}