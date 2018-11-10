package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;

/**
 * @author Citizen-Cane
 *
 */
public class HeadGestureInputMethod extends AbstractInputMethod {
    private final Future<MotionDetector> motionDetector;

    public HeadGestureInputMethod(ExecutorService executorService, Supplier<MotionDetector> motionDetector) {
        super(executorService);
        this.motionDetector = executor.submit(motionDetector::get);
    }

    private static final List<Gesture> SupportedGestures = Arrays.asList(Gesture.Nod, Gesture.Shake);

    @Override
    protected int handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        return awaitGesture(motionDetector.get(), prompt);
    }

    private static int awaitGesture(MotionDetector motionDetector, Prompt prompt) {
        return motionDetector.call(() -> {
            motionDetector.setSensitivity(MotionSensitivity.High);
            List<Gesture> gestures = prompt.choices.toGestures();
            while (!Thread.currentThread().isInterrupted()) {
                Gesture gesture = motionDetector.await(gestures, Double.MAX_VALUE);
                if (supported(gesture)) {
                    int result = gestures.indexOf(gesture);
                    if (result >= 0 && result < prompt.choices.size()) {
                        return result;
                    }
                }
            }
            return Prompt.UNDEFINED;
        });
    }

    private static boolean supported(Gesture gesture) {
        int result = SupportedGestures.indexOf(gesture);
        return 0 <= result && result < SupportedGestures.size();
    }

    @Override
    protected boolean handleDismiss(Prompt prompt) throws InterruptedException {
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return motionDetector.toString();
    }
}
