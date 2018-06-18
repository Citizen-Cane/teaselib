package teaselib.core.devices.motiondetection;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.Signal;

/**
 * @author Citizen-Cane
 *
 */
public abstract class PerceptionSource<T> {
    static final Logger logger = LoggerFactory.getLogger(PerceptionSource.class);

    @FunctionalInterface
    public interface PerceptionChanged {
        boolean expected(MotionDetectionResult result);
    }

    final AtomicReference<T> current;
    final Signal signal;

    public PerceptionSource() {
        this.current = new AtomicReference<>();
        this.signal = new Signal();
    }

    public abstract void update(Mat video, long timeStamp);

    public T await(List<T> expected, double timeoutSeconds) {
        if (!active()) {
            throw new IllegalStateException(getClass().getName() + " not active");
        }

        try {
            if (matches(expected)) {
                return current.get();
            }
            try {
                signal.await(timeoutSeconds, (() -> {
                    if (matches(expected)) {
                        startNewRecognition();
                        return true;
                    } else {
                        return false;
                    }
                }));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return current.get();
        } finally {
            resetCurrent();
        }

    }

    private boolean matches(List<T> expected) {
        return expected.contains(current.get());
    }

    abstract boolean active();

    abstract void startNewRecognition();

    abstract void resetCurrent();
}
