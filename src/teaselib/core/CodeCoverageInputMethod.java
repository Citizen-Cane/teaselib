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
import teaselib.core.util.ExceptionUtil;

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

    CyclicBarrier checkPointScriptFunctionStarted = new CyclicBarrier(2);
    CyclicBarrier checkPointScriptFunctionFinished = new CyclicBarrier(2);

    @Override
    public int handleShow(Prompt prompt) throws InterruptedException {
        activePrompt.set(prompt);
        result.set(Prompt.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            try {
                checkPointScriptFunctionStarted.await();
                checkPointScriptFunctionFinished.await();
                return result.get();
            } catch (BrokenBarrierException e) {
                throw new InterruptedException();
            }
        } else {
            return firePromptShown(activePrompt.get());
        }
    }

    private void handleCheckPointReached(CheckPoint checkPoint) {
        if (checkPoint == CheckPoint.ScriptFunction.Started) {
            synchronizeWithPrompt();
            activePrompt.getAndUpdate(this::forwardResult);
        } else if (checkPoint == CheckPoint.Script.NewMessage) {
            activePrompt.getAndUpdate(this::forwardResult);
            // TODO Should work but doesn't
            // activePrompt.getAndUpdate(this::setAndForwardResult);
        } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            activePrompt.getAndUpdate(this::forwardResult);
            // TODO Blocks script function without advance time event
            // - we don't know if there are any time advances pending so we have to wait
            awaitPendingAdvanceTimeEvents();
        }
    }

    private void handleTimeAdvance(@SuppressWarnings("unused") TimeAdvancedEvent timeAdvancedEvent) {
        activePrompt.getAndUpdate(this::setResult);
        // TODO should work but doesn't
        // activePrompt.getAndUpdate(this::setAndForwardResult);
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

    private boolean hasScriptFunction(Prompt prompt) {
        return prompt != null && prompt.hasScriptFunction();
    }

    private boolean resultNotSet() {
        return result.get() == Prompt.UNDEFINED;
    }

    private boolean resultSet() {
        return result.get() != Prompt.UNDEFINED;
    }

    private void synchronizeWithPrompt() {
        try {
            checkPointScriptFunctionStarted.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private void forwardResult() {
        try {
            checkPointScriptFunctionFinished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private void awaitPendingAdvanceTimeEvents() {
        synchronized (this) {
            if (!Thread.currentThread().isInterrupted()) {
                try {
                    while (true) {
                        wait(Integer.MAX_VALUE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public boolean handleDismiss(Prompt prompt) {
        activePrompt.set(null);
        checkPointScriptFunctionStarted.reset();
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
