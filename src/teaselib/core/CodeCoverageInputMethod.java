package teaselib.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.debug.TimeAdvancedEvent;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod extends AbstractInputMethod implements DebugInputMethod {
    Set<InputMethod.Listener> eventListeners = new LinkedHashSet<>();

    private final TimeAdvanceListener timeAdvanceListener = this::handleTimeAdvance;
    private final CheckPointListener checkPointListener = this::handleCheckPointReached;

    public CodeCoverageInputMethod(ExecutorService executor) {
        super(executor);
    }

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    private final AtomicInteger result = new AtomicInteger();

    CyclicBarrier checkPointScriptFunctionFinished = new CyclicBarrier(2);

    @Override
    public int handleShow(Prompt prompt) throws InterruptedException {
        activePrompt.set(prompt);
        result.set(Prompt.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            try {
                checkPointScriptFunctionFinished.await();
                return result.get();
            } catch (BrokenBarrierException e) {
                throw new InterruptedException();
            } finally {
                activePrompt.set(null);
                checkPointScriptFunctionFinished.reset();
            }
        } else {
            return firePromptShown(activePrompt.get());
        }
    }

    private void handleCheckPointReached(CheckPoint checkPoint) {
        if (checkPoint == CheckPoint.ScriptFunction.Started) {
            activePrompt.getAndUpdate(this::forwardResult);
            // TODO Resolves infinite wait at end - awaitPendingAdvanceTimeEvents
            // -> however timeout test would be indeterministic
            // activePrompt.getAndUpdate(this::setAndForwardResult);
        } else if (checkPoint == CheckPoint.Script.NewMessage) {
            activePrompt.getAndUpdate(this::forwardResult);
            // TODO Should work but doesn't - sometimes script function blocks
            // activePrompt.getAndUpdate(this::setAndForwardResult);
        } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            activePrompt.getAndUpdate(this::forwardResultAndHandleTimeout);
        }
    }

    private void handleTimeAdvance(@SuppressWarnings("unused") TimeAdvancedEvent timeAdvancedEvent) {
        // activePrompt.getAndUpdate(this::setResult);
        // TODO should work but doesn't
        // TODO Since forward has to wait until the prompt is ready,
        // a broken barrier exception will be thrown into the script thread
        activePrompt.getAndUpdate(this::setAndForwardResult);
    }

    Prompt setResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultNotSet()) {
            result.set(firePromptShown(prompt));
            return prompt;
        } else {
            return prompt;
        }
    }

    Prompt setAndForwardResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultNotSet()) {
            result.set(firePromptShown(prompt));
            forwardResult();
            return null;
        } else {
            return prompt;
        }
    }

    Prompt forwardResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultSet()) {
            forwardResult();
            return null;
        } else {
            return prompt;
        }
    }

    private static boolean hasScriptFunction(Prompt prompt) {
        return prompt != null && prompt.hasScriptFunction();
    }

    private boolean resultNotSet() {
        return result.get() == Prompt.UNDEFINED;
    }

    private boolean resultSet() {
        return result.get() != Prompt.UNDEFINED;
    }

    private void forwardResult() {
        try {
            synchronized (this) {
                if (!Thread.currentThread().isInterrupted()) {
                    checkPointScriptFunctionFinished.await();
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }

    Prompt forwardResultAndHandleTimeout(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultSet()) {
            forwardResult();
            return null;
        } else {
            // TODO Must be here instead of TimeAdvance
            if (!Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    try {
                        // TODO Blocks since the prompt isn0t dismissed,
                        // and on timeouts the script function is never interrupted
                        // TODO If not waiting at all, there's no chance for code coverage
                        // to dismiss before the timeout triggers

                        // while (!Thread.currentThread().isInterrupted()) {
                        // wait(Integer.MAX_VALUE);
                        // }

                        // TODO Resolve ugly workaround
                        wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return prompt;
    }

    @Override
    public boolean handleDismiss(Prompt prompt) {
        activePrompt.set(null);
        checkPointScriptFunctionFinished.reset();

        firePromptDismissed(prompt);
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return new HashMap<>();
    }

    @Override
    public void attach(TeaseLib teaseLib) {
        teaseLib.addTimeAdvancedListener(timeAdvanceListener);
        teaseLib.addCheckPointListener(checkPointListener);
    }

    @Override
    public void detach(TeaseLib teaseLib) {
        teaseLib.removeTimeAdvancedListener(timeAdvanceListener);
        teaseLib.removeCheckPointListener(checkPointListener);
    }

    public void addEventListener(InputMethod.Listener e) {
        eventListeners.add(e);
    }

    public void removeEventListener(InputMethod.Listener e) {
        eventListeners.remove(e);
    }

    private int firePromptShown(Prompt prompt) {
        Optional<Integer> listenerResult = eventListeners.stream().map(e -> e.promptShown(prompt))
                .reduce(CodeCoverageInputMethod::firstResult);
        return listenerResult.isPresent() ? listenerResult.get() : Prompt.UNDEFINED;
    }

    private static int firstResult(int a, int b) {
        return a == Prompt.UNDEFINED ? b : a;
    }

    private void firePromptDismissed(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptDismissed(prompt));
    }
}
