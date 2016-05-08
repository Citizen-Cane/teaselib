package teaselib.core.devices.motiondetection;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.motiondetection.MotionDetector;

public abstract class BasicMotionDetector implements MotionDetector {

    protected static final double MotionInertiaSeconds = 0.5;
    protected static final int MaximumNumberOfPastFrames = 400;

    public BasicMotionDetector() {
    }

    protected abstract class DetectionEvents extends Thread {
        public final Lock lockStartStop = new ReentrantLock();
    }

    protected int frames(double seconds) {
        return Math.max(1,
                Math.min((int) (fps() * seconds), MaximumNumberOfPastFrames));
    }

    protected abstract double fps();

}
