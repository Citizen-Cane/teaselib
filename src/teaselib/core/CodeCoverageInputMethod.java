package teaselib.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.debug.TimeAdvancedEvent;
import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod extends AbstractInputMethod implements DebugInputMethod {
    Set<InputMethod.Listener> eventListeners = new LinkedHashSet<>();

    private final CheckPointListener checkPointListener = this::handleCheckPointReached;
    private final TimeAdvanceListener timeAdvanceListener = this::handleTimeAdvance;

    private final AtomicReference<Prompt.Result> result = new AtomicReference<>();
    private final CyclicBarrier checkPointScriptFunctionFinished = new CyclicBarrier(2);

    public CodeCoverageInputMethod(ExecutorService executor) {
        super(executor);
    }

    @Override
    public Setup getSetup(Choices choices) {
        return Unused;
    }

    @Override
    public Prompt.Result handleShow(Prompt prompt) throws InterruptedException {
        // activePrompt.set(prompt);
        result.set(Prompt.Result.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            Prompt.Result choice = firePromptShown(prompt);
            if (choice.valid(prompt.choices)) {
                result.set(choice);
            }
            try {
                checkPointScriptFunctionFinished.await();
                return result.get();
            } catch (BrokenBarrierException e) {
                throw new InterruptedException();
            } finally {
                // activePrompt.set(null);
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
            activePrompt.getAndUpdate(this::forwardResultAndHandleTimeout);
        }
    }

    private void handleTimeAdvance(@SuppressWarnings("unused") TimeAdvancedEvent timeAdvancedEvent) {
        // Ignore
    }

    private static boolean hasScriptFunction(Prompt prompt) {
        return prompt != null && prompt.hasScriptFunction();
    }

    private boolean resultNotSet() {
        return !resultSet();
    }

    private boolean resultSet() {
        return result.get().valid(activePrompt.get().choices);
    }

    private Prompt forwardResultAndHandleTimeout(Prompt prompt) {
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

    @Override
    public boolean handleDismiss(Prompt prompt) {
        synchronized (this) {
            // activePrompt.set(null);
            checkPointScriptFunctionFinished.reset();
            firePromptDismissed(prompt);
            notifyAll();
        }
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
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

    private Prompt.Result firePromptShown(Prompt prompt) {
        Optional<Prompt.Result> listenerResult = eventListeners.stream().map(e -> e.promptShown(prompt))
                .reduce(CodeCoverageInputMethod::firstResult);
        return listenerResult.isPresent() ? listenerResult.get() : Prompt.Result.UNDEFINED;
    }

    private static Prompt.Result firstResult(Prompt.Result a, Prompt.Result b) {
        return a == Prompt.Result.UNDEFINED ? b : a;
    }

    private void firePromptDismissed(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptDismissed(prompt));
    }

}
