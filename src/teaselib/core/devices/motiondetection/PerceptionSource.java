package teaselib.core.devices.motiondetection;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;
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
    public interface PerceptionFunction {
        boolean matches();
    }

    final AtomicReference<T> current;
    final Signal signal;

    public PerceptionSource() {
        this.current = new AtomicReference<>();
        this.signal = new Signal();
    }

    public void setResulution(Size resolution) {
        // Overwrite if necessary
    };

    public abstract void update(Mat video, long timeStamp);

    public boolean await(List<T> expected, double timeoutSeconds) {
        return await(() -> matches(expected), timeoutSeconds);
    }

    public boolean await(PerceptionFunction expected, double timeoutSeconds) {
        // TODO Move out, it's the responsibility of the capture thread
        if (!active()) {
            throw new IllegalStateException(getClass().getName() + " not active");
        }

        try {
            if (expected.matches()) {
                return true;
            }
            try {
                if (signal.await(timeoutSeconds, (() -> {
                    if (expected.matches()) {
                        startNewRecognition();
                        return true;
                    } else {
                        return false;
                    }
                }))) {
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } finally {
            resetCurrent();
        }
        return false;
    }

    public T get() {
        return current.get();
    }

    private boolean matches(List<T> expected) {
        return expected.contains(current.get());
    }

    public static Rect defaultRegion(Mat video) {
        return new Rect(video.cols() / 4, video.rows() / 4, video.cols() / 2, video.rows() / 2);
    }

    abstract boolean active();

    abstract void startNewRecognition();

    abstract void resetCurrent();

}
