package teaselib.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod extends AbstractInputMethod implements DebugInputMethod {
    Set<InputMethod.Listener> eventListeners = new LinkedHashSet<>();

    public CodeCoverageInputMethod(ExecutorService executor) {
        super(executor);
    }

    @Override
    public int handleShow(Prompt prompt) {
        return firePromptShown(prompt);
        // TODO script function timeout
    }

    @Override
    public boolean handleDismiss(Prompt prompt) throws InterruptedException {
        firePromptDismissed(prompt);
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return new HashMap<>();
    }

    @Override
    public void attach(TeaseLib teaseLib) {
        // Ignore
    }

    @Override
    public void detach(TeaseLib teaseLib) {
        // Ignore
    }

    public void addEventListener(InputMethod.Listener e) {
        eventListeners.add(e);
    }

    public void removeEventListener(InputMethod.Listener e) {
        eventListeners.remove(e);
    }

    private int firePromptShown(Prompt prompt) {
        Optional<Integer> result = eventListeners.stream().map(e -> e.promptShown(prompt))
                .reduce(CodeCoverageInputMethod::firstResult);
        return result.isPresent() ? result.get() : Prompt.UNDEFINED;
    }

    private static int firstResult(int a, int b) {
        return a == Prompt.UNDEFINED ? b : a;
    }

    private void firePromptDismissed(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptDismissed(prompt));
    }
}
