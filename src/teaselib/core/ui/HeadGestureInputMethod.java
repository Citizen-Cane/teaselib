/**
 * 
 */
package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;

/**
 * @author Citizen-Cane
 *
 */
public class HeadGestureInputMethod implements InputMethod {
    private final Supplier<MotionDetector> motionDetector;

    public HeadGestureInputMethod(Supplier<MotionDetector> motionDetector) {
        this.motionDetector = motionDetector;
    }

    private final NamedExecutorService workerThread = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final ReentrantLock replySection = new ReentrantLock(true);
    private Future<Integer> gestureResult;

    private static final List<Gesture> SupportedGestures = Arrays.asList(Gesture.Nod, Gesture.Shake);

    @Override
    public void show(Prompt prompt) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                replySection.lockInterruptibly();
                try {
                    synchronized (this) {
                        notifyAll();
                    }
                    if (prompt.result() == Prompt.UNDEFINED) {
                        int result = awaitGesture(motionDetector.get(), prompt);
                        prompt.lock.lockInterruptibly();
                        try {
                            if (!prompt.paused() && prompt.result() == Prompt.UNDEFINED) {
                                prompt.signalResult(result);
                            }
                        } finally {
                            prompt.lock.unlock();
                        }
                    }
                } finally {
                    replySection.unlock();
                }
                return prompt.result();
            }

        };

        synchronized (callable) {
            gestureResult = workerThread.submit(callable);
            while (!replySection.isLocked()) {
                callable.wait();
            }
        }
    }

    private int awaitGesture(MotionDetector motionDetector, Prompt prompt) {
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

    private boolean supported(Gesture gesture) {
        int result = SupportedGestures.indexOf(gesture);
        return 0 <= result && result < SupportedGestures.size();
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        if (gestureResult.isCancelled() || gestureResult.isDone()) {
            return false;
        }

        gestureResult.cancel(true);
        replySection.lockInterruptibly();
        replySection.unlock();

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
