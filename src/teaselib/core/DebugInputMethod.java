package teaselib.core;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Debugger.Response;
import teaselib.core.debug.DebugResponses;
import teaselib.core.debug.DebugResponses.Result;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class DebugInputMethod implements InputMethod {
    private static final Logger logger = LoggerFactory.getLogger(DebugInputMethod.class);

    final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    final AtomicLong elapsed = new AtomicLong();

    private final DebugResponses responses = new DebugResponses();

    private final TimeAdvanceListener timeAdvanceListener = e -> {
        Prompt prompt = activePrompt.get();
        if (prompt != null) {
            dismissExpectedPromptOrIgnore(prompt);

            elapsed.set(elapsed.get() + e.teaseLib.getTime(TimeUnit.MILLISECONDS));
        }
    };

    private void dismissExpectedPromptOrIgnore(Prompt prompt) {
        prompt.lock.lock();
        try {
            Result result = responses.getResponse(prompt.choices);
            if (result.response == Response.Choose) {
                logger.info("Choosing " + result);
                prompt.setResultOnce(result.index);
                prompt.click.signalAll();
            } else {
                logger.info("Ignoring " + result);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public void show(Prompt prompt) {
        dismissExpectedPromptOrShow(prompt);
    }

    private void dismissExpectedPromptOrShow(Prompt prompt) {
        prompt.lock.lock();
        try {
            Result result = responses.getResponse(prompt.choices);
            if (result.response == Response.Choose) {
                logger.info("Choosing " + result);
                prompt.setResultOnce(result.index);
                prompt.click.signalAll();
            } else {
                activePrompt.set(prompt);
                elapsed.set(0);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        activePrompt.set(null);
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }

    public DebugResponses getResponses() {
        return responses;
    }

    public void attach(TeaseLib teaseLib) {
        teaseLib.addTimeAdvancedListener(timeAdvanceListener);
    }

    public void detach(TeaseLib teaseLib) {
        teaseLib.removeTimeAdvancedListener(timeAdvanceListener);
    }
}
