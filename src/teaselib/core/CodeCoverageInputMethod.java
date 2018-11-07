package teaselib.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod implements DebugInputMethod {
    private static final Logger logger = LoggerFactory.getLogger(CodeCoverageInputMethod.class);

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    Set<InputMethod.Listener> eventListeners = new LinkedHashSet<>();

    @Override
    public void show(Prompt prompt) {
        firePromptShown(prompt);

        // TODO script functions timeout
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        firePromptDismissed(prompt);
        activePrompt.set(null);
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<>();
        return handlers;
    }

    @Override
    public void attach(TeaseLib teaseLib) {
    }

    @Override
    public void detach(TeaseLib teaseLib) {
    }

    public void addEventListener(InputMethod.Listener e) {
        eventListeners.add(e);
    }

    public void removeEventListener(InputMethod.Listener e) {
        eventListeners.remove(e);
    }

    private void firePromptShown(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptShown(prompt));
    }

    private void firePromptDismissed(Prompt prompt) {
        eventListeners.stream().forEach(e -> e.promptDismissed(prompt));
    }

    @Override
    public String toString() {
        Prompt prompt = activePrompt.get();
        if (prompt != null) {
            return prompt.toString();
        } else {
            return "<no active prompt>";
        }
    }
}
