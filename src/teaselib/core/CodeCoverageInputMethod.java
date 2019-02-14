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
            // Ignore
        } else if (checkPoint == CheckPoint.Script.NewMessage) {
            // Ignore
        } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            activePrompt.getAndUpdate(this::setResult);
            activePrompt.getAndUpdate(this::forwardResultAndHandleTimeout);
        }
    }

    private void handleTimeAdvance(TimeAdvancedEvent timeAdvancedEvent) {
        // Ignore
    }

    Prompt setResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultNotSet()) {
            int choice = firePromptShown(prompt);
            if (choice > Prompt.DISMISSED) {
                result.set(choice);
            }
            return prompt;
        } else {
            return prompt;
        }
    }

    Prompt setAndForwardResult(Prompt prompt) {
        if (hasScriptFunction(prompt) && resultNotSet()) {
            int choice = firePromptShown(prompt);
            if (choice > Prompt.DISMISSED) {
                result.set(choice);
                forwardResult();
            }
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
        return result.get() <= Prompt.DISMISSED;
    }

    private boolean resultSet() {
        return result.get() > Prompt.DISMISSED;
    }

    private void forwardResult() {
        try {
            if (resultNotSet()) {
                throw new IllegalArgumentException("Result must be set to choice");
            }
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
            synchronized (this) {
                if (resultSet()) {
                    forwardResult();
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return null;
        } else {
            return prompt;
        }
    }

    @Override
    public boolean handleDismiss(Prompt prompt) {
        synchronized (this) {
            activePrompt.set(null);
            checkPointScriptFunctionFinished.reset();
            firePromptDismissed(prompt);
            notifyAll();
        }
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
