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

    CyclicBarrier rendevouz = new CyclicBarrier(2);

    @Override
    public int handleShow(Prompt prompt) {
        activePrompt.set(prompt);
        result.set(Prompt.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            try {
                synchronized (this) {
                    rendevouz.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                // Ignore
            }
            return result.get();
        } else {
            return firePromptShown(activePrompt.get());
        }
    }

    private void handleCheckPointReached(CheckPoint checkPoint) {
        if (checkPoint == CheckPoint.ScriptFunction.Started) {
            try {
                Prompt prompt = activePrompt.getAndSet(null);
                result.set(firePromptShown(prompt));
                rendevouz.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } else if (checkPoint == CheckPoint.ScriptFunction.Finished) {
            rendevouz.reset();
        }
    }

    private void handleTimeAdvance(TimeAdvancedEvent e) {
        // Prompt prompt = activePrompt.get();
        // if (prompt != null && prompt.hasScriptFunction()) {
        // result.set(firePromptShown(prompt));
        // activePrompt.set(null);
        // }
    }

    @Override
    public boolean handleDismiss(Prompt prompt) {
        activePrompt.set(null);
        rendevouz.reset();
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
