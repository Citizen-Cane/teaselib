package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.Script;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public int show(Script script, Prompt prompt) throws InterruptedException {
        Prompt activePrompt = active.get();

        if (activePrompt != null) {
            if (prompt != activePrompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            } else {
                pause(activePrompt);
            }
        }

        makePromptActive(prompt);

        if (prompt.scriptFunction != null) {
            prompt.executeScriptTask(script);
        }

        return showExisting(prompt);
    }

    public int showExisting(Prompt prompt) throws InterruptedException {
        if (prompt.result() == Prompt.UNDEFINED) {
            prompt.click.await();
        }
        dismissPrompt(prompt);
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

        if (prompt.result() != Prompt.UNDEFINED)
            return;

        prompt.resume();
        makePromptActive(prompt);
    }

    private void makePromptActive(Prompt prompt) throws InterruptedException {
        active.set(prompt);
        for (InputMethod inputMethod : prompt.inputMethods) {
            inputMethod.show(prompt);
        }
    }

    public boolean dismiss(Prompt prompt) {
        if (prompt.result() == Prompt.UNDEFINED) {
            prompt.setResultOnce(Prompt.DISMISSED);
            return dismissPrompt(prompt);
        } else {
            return false;
        }
    }

    public boolean pause(Prompt prompt) {
        prompt.pause();
        if (prompt.result() == Prompt.UNDEFINED) {
            return dismissPrompt(prompt);
        } else {
            return false;
        }
    }

    private boolean dismissPrompt(Prompt prompt) {
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
