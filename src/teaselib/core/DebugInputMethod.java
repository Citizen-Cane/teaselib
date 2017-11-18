package teaselib.core;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    final AtomicReference<Prompt> activePrompt = new AtomicReference<>();
    final AtomicLong elapsed = new AtomicLong();

    private final DebugResponses responses = new DebugResponses();

    private final TimeAdvanceListener timeAdvanceListener = e -> {
        Prompt prompt = activePrompt.get();
        if (prompt != null) {
            if (responses.getResponse(prompt.choices).response == Response.Choose) {
                prompt.lock.lock();
                try {
                    prompt.click.signalAll();
                } finally {
                    prompt.lock.unlock();
                }
            }
            
            elapsed.set(elapsed.get() + e.teaseLib.getTime(TimeUnit.MILLISECONDS));
        }
    };

    @Override
    public void show(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            Result result = responses.getResponse(prompt.choices);
            if (result.response == Response.Choose) {
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
