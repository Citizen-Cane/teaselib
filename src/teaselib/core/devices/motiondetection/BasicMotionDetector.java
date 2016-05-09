package teaselib.core.devices.motiondetection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.TeaseLib;
import teaselib.motiondetection.MotionDetector;

public abstract class BasicMotionDetector implements MotionDetector {

    protected static final int MotionInertia = 4; // frames
    protected static final int MaximumNumberOfPastFrames = 400;

    protected DetectionEvents detectionEvents = null;

    public BasicMotionDetector() {
        this.detectionEvents = null;
    }

    protected abstract class DetectionEvents extends Thread {
        final Lock motionStartLock = new ReentrantLock();
        final Condition motionStart = motionStartLock.newCondition();
        final Lock motionEndLock = new ReentrantLock();
        final Condition motionEnd = motionEndLock.newCondition();
        public final Lock lockStartStop = new ReentrantLock();

        protected DetectionEvents() {
            super();
            setName("JavaCV Motion detector events");
        }

        protected void signalMotionStart() {
            motionStartLock.lock();
            try {
                TeaseLib.instance().log.info("Motion started");
                motionStart.signalAll();
            } finally {
                motionStartLock.unlock();
            }
        }

        protected void signalMotionEnd() {
            motionEndLock.lock();
            try {
                TeaseLib.instance().log.info("Motion ended");
                motionEnd.signalAll();
            } finally {
                motionEndLock.unlock();
            }
        }
    }

    protected int frames(double seconds) {
        return Math.max(1,
                Math.min((int) (fps() * seconds), MaximumNumberOfPastFrames));
    }

    protected abstract int fps();

    @Override
    public boolean isMotionDetected(double pastSeconds) {
        return isMotionDetected(frames(pastSeconds));
    }

    protected abstract boolean isMotionDetected(int pastFrames);

    @Override
    public boolean awaitMotionStart(double timeoutSeconds) {
        detectionEvents.motionStartLock.lock();
        try {
            boolean motionDetected = isMotionDetected(MotionInertia);
            if (!motionDetected) {
                motionDetected = detectionEvents.motionStart.await(
                        (long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
            return motionDetected;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            detectionEvents.motionStartLock.unlock();
        }
    }

    @Override
    public boolean awaitMotionEnd(double timeoutSeconds) {
        detectionEvents.motionEndLock.lock();
        try {
            boolean motionStopped = !isMotionDetected(MotionInertia);
            if (!motionStopped) {
                motionStopped = detectionEvents.motionEnd.await(
                        (long) timeoutSeconds * 1000, TimeUnit.MILLISECONDS);
            }
            return motionStopped;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            detectionEvents.motionEndLock.unlock();
        }
    }

    @Override
    public boolean active() {
        return detectionEvents != null;
    }

    public void pause() {
        detectionEvents.lockStartStop.lock();
    }

    public void resume() {
        detectionEvents.lockStartStop.unlock();
    }

    @Override
    public void release() {
        detectionEvents.interrupt();
        try {
            detectionEvents.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
