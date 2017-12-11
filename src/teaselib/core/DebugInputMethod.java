package teaselib.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Debugger.Response;
import teaselib.core.Debugger.ResponseAction;
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

    private static final String DEBUG_INPUT_METHOD_HANDLER = "DebugInputMethodHandler";

    private final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    private final AtomicLong elapsed = new AtomicLong();

    private final DebugResponses responses = new DebugResponses();
    private final Runnable debugInputMethodHandler;

    public DebugInputMethod(Runnable debugInputMethodHandler) {
        this.debugInputMethodHandler = debugInputMethodHandler;
    }

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
                choose(prompt, result);
            } else if (result.response == Response.Invoke) {
                invokeHandlerOnce(prompt, result);
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
        Result result = responses.getResponse(prompt.choices);
        if (result.response == Response.Choose) {
            choose(prompt, result);
        } else if (result.response == Response.Invoke) {
            invokeHandlerOnce(prompt, result);
        } else {
            activePrompt.set(prompt);
            elapsed.set(0);
        }
    }

    private void invokeHandlerOnce(Prompt prompt, Result result) {
        new Thread(() -> {
            prompt.lock.lock();
            try {
                prompt.signalHandlerInvocation(DEBUG_INPUT_METHOD_HANDLER);
                responses.replace(new ResponseAction(result.match, Response.Choose));
            } finally {
                prompt.lock.unlock();
            }
        }, "Debug Input Method Handler").start();
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        activePrompt.set(null);
        return true;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        HashMap<String, Runnable> handlers = new HashMap<>();
        handlers.put(DEBUG_INPUT_METHOD_HANDLER, debugInputMethodHandler);
        return handlers;
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

    public void replyScriptFunction(String match) {
        Prompt prompt = activePrompt.get();
        if (prompt == null) {
            throw new IllegalStateException("No active prompt: " + match);
        } else {
            prompt.lock.lock();
            try {
                Result result = DebugResponses.getResponse(prompt.choices, new ResponseAction(match), null);
                if (result != null) {
                    choose(prompt, result);
                    try {
                        prompt.click.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new IllegalArgumentException("Prompt " + prompt + " doesn't match '" + match + "'");
                }
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    private static void choose(Prompt prompt, Result result) {
        logger.info("Choosing " + result);
        prompt.signalResult(result.index);
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
