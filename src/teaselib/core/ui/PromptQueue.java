package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<>();

    void show(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.get();

        if (prompt == activePrompt) {
            throw new IllegalStateException("Already showing " + prompt);
        } else if (activePrompt != null) {
            pause(activePrompt);
        }

        try {
            activate(prompt);
        } catch (Exception e) {
            if (prompt.hasScriptTask()) {
                prompt.cancelScriptTask();
            }
            throw ExceptionUtil.asRuntimeException(e);
        }

        if (prompt.hasScriptTask()) {
            prompt.executeScriptTask();
        }

        awaitResult(prompt);
    }

    void awaitResult(Prompt prompt) throws InterruptedException {
        if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
            prompt.click.await();
        }
        dismiss(prompt);
    }

    void resume(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.get();

        if (activePrompt != null) {
            if (prompt == activePrompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            } else {
                throw new IllegalStateException("Previous prompt " + activePrompt + " still showing");
            }
        }

        if (!prompt.result().equals(Prompt.Result.UNDEFINED))
            return;

        prompt.resume();
        activate(prompt);
    }

    private void activate(Prompt prompt) throws InterruptedException {
        prompt.show();
        active.set(prompt);
    }

    void pause(Prompt prompt) {
        prompt.pause();
        if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
            dismiss(prompt);
        } else {
            throw new IllegalStateException("Previous prompt already dismissed: " + prompt);
        }
    }

    void dismiss(Prompt prompt) {
        Prompt activePrompt = active.get();

        if (activePrompt == null) {
            throw new IllegalArgumentException("Prompt already dismissed: " + prompt);
        }
        if (prompt != activePrompt) {
            throw new IllegalArgumentException("Can only dismiss active prompt: " + activePrompt);
        }

        try {
            prompt.dismiss();
        } finally {
            active.set(null);
        }
    }

    void setActive(Prompt prompt) {
        active.set(prompt);
    }

    Prompt getActive() {
        return active.get();
    }

    @Override
    public String toString() {
        return active.toString();
    }

}
