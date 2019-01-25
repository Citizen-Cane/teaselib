package teaselib.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    private final TimeAdvanceListener timeAdvanceListener = e -> {
        handleTimeAdvance(e);
    };

    public CodeCoverageInputMethod(ExecutorService executor) {
        super(executor);
    }

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    private final AtomicInteger result = new AtomicInteger();

    @Override
    public int handleShow(Prompt prompt) {
        activePrompt.set(prompt);
        result.set(Prompt.UNDEFINED);
        if (prompt.hasScriptFunction()) {
            try {
                synchronized (this) {
                    while (result.get() < Prompt.DISMISSED) {
                        wait(Integer.MAX_VALUE);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result.get();
        } else {
            return firePromptShown(activePrompt.get());
        }
    }

    private void handleTimeAdvance(TimeAdvancedEvent e) {
        Prompt prompt = activePrompt.get();
        if (prompt != null && prompt.hasScriptFunction()) {
            result.set(firePromptShown(prompt));
            activePrompt.set(null);
            synchronized (this) {
                notifyAll();
            }
        }
    }

    @Override
    public boolean handleDismiss(Prompt prompt) throws InterruptedException {
        activePrompt.set(null);
        firePromptDismissed(prompt);
        synchronized (this) {
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
    }

    @Override
    public void detach(TeaseLib teaseLib) {
        teaseLib.removeTimeAdvancedListener(timeAdvanceListener);
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
