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

import teaselib.core.concurrency.NamedExecutorService;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;

/**
 * @author Citizen-Cane
 *
 */
public class HeadGestureInputMethod implements InputMethod {

    private final MotionDetector motionDetector;

    public HeadGestureInputMethod(MotionDetector motionDetector) {
        super();
        this.motionDetector = motionDetector;
    }

    private final NamedExecutorService workerThread = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final ReentrantLock replySection = new ReentrantLock(true);
    private Future<Integer> gestureResult;

    private static final List<Gesture> SupportedGestures = Arrays.asList(Gesture.Nod, Gesture.Shake);

    @Override
    public void show(final Prompt prompt) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                replySection.lockInterruptibly();
                try {
                    synchronized (this) {
                        notifyAll();
                    }
                    if (prompt.result() == Prompt.UNDEFINED) {
                        Gesture gesture = Gesture.None;
                        while (gesture == Gesture.None) {
                            gesture = motionDetector.await(Gesture.Nod, Double.MAX_VALUE);
                        }
                        prompt.lock.lockInterruptibly();
                        try {
                            if (!prompt.paused() && prompt.result() == Prompt.UNDEFINED) {
                                int result = SupportedGestures.indexOf(gesture);
                                if (0 <= result && result < SupportedGestures.size()) {
                                    prompt.signalResult(result);
                                }
                            }
                        } finally {
                            prompt.lock.unlock();
                        }
                    } else {
                        // Ignored because another input method might have dismissed the prompt
                    }
                } finally {
                    replySection.unlock();
                }
                return prompt.result();
            }
        };

        synchronized (callable) {
            gestureResult = workerThread.submit(callable);
            callable.wait();
        }
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
