package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import teaselib.Answer.Meaning;
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
    protected Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        return awaitGesture(motionDetector.get(), prompt);
    }

    private static Prompt.Result awaitGesture(MotionDetector motionDetector, Prompt prompt) {
        return motionDetector.call(() -> {
            motionDetector.setSensitivity(MotionSensitivity.High);
            List<Gesture> gestures = prompt.choices.stream().map(HeadGestureInputMethod::gesture)
                    .collect(Collectors.toList());
            while (!Thread.currentThread().isInterrupted()) {
                Gesture gesture = motionDetector.await(gestures, Double.MAX_VALUE);
                if (supported(gesture)) {
                    Prompt.Result result = new Prompt.Result(gestures.indexOf(gesture));
                    if (result.valid(prompt.choices)) {
                        return result;
                    }
                }
            }
            return Prompt.Result.UNDEFINED;
        });
    }

    private static Gesture gesture(Choice choice) {
        if (choice.answer.meaning == Meaning.YES)
            return Gesture.Nod;
        else if (choice.answer.meaning == Meaning.NO)
            return Gesture.Shake;
        else
            return Gesture.None;
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
