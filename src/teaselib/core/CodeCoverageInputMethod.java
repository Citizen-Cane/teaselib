package teaselib.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class CodeCoverageInputMethod implements DebugInputMethod {
    private static final Logger logger = LoggerFactory.getLogger(CodeCoverageInputMethod.class);

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    public CodeCoverageInputMethod() {
    }

    @Override
    public void show(Prompt prompt) {
        // TODO script functions timeout
        // TODO Manual hints for script functions
        logger.info("Choosing {}", prompt.choices.get(0));
        prompt.signalResult(this, 0);
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
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
