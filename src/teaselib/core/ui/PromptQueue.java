package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public Prompt.Result show(Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.get();

        if (prompt == activePrompt) {
            throw new IllegalStateException("Already showing " + prompt);
        } else if (activePrompt != null) {
            pause(activePrompt);
        }

        try {
            activate(prompt);
        } catch (Exception e) {
            dismiss(prompt);
        }

        if (prompt.scriptTask != null) {
            prompt.executeScriptTask();
        }

        return awaitResult(prompt);
    }

    public Prompt.Result awaitResult(Prompt prompt) throws InterruptedException {
        if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
            prompt.click.await();
        }

        if (prompt.result().equals(Prompt.Result.UNDEFINED) && !prompt.paused()) {
            prompt.setTimedOut();
        }

        dismiss(prompt);
        return prompt.result();
    }

    public void resume(Prompt prompt) throws InterruptedException {
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
        active.set(prompt);
        for (InputMethod inputMethod : prompt.inputMethods) {
            inputMethod.show(prompt);
        }
    }

    public boolean pause(Prompt prompt) {
        prompt.pause();
        if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
            return dismiss(prompt);
        } else {
            return false;
        }
    }

    private boolean dismiss(Prompt prompt) {
        Prompt activePrompt = active.get();

        if (activePrompt == null) {
            throw new IllegalArgumentException("Prompt already dismissed: " + prompt);
        }
        if (prompt != activePrompt) {
            throw new IllegalArgumentException("Can only dismiss active prompt: " + activePrompt);
        }

        try {
            return prompt.dismiss();
        } finally {
            active.set(null);
        }
    }

    public Prompt getActive() {
        return active.get();
    }
}
