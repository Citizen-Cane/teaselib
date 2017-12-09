package teaselib.core.ui;

import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.Script;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
    private final AtomicReference<Prompt> active = new AtomicReference<>();

    public int show(Script script, Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            // Prevent multiple entry while initializing prompt
            synchronized (this) {
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
            }

            return showExisting(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }

    public int showExisting(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            try {
                if (prompt.result() == Prompt.UNDEFINED) {
                    prompt.click.await();
                }
            } finally {
                dismissPrompt(prompt);
            }

            if (prompt.exception != null) {
                if (prompt.exception instanceof Error) {
                    throw (Error) prompt.exception;
                } else {
                    throw ExceptionUtil.asRuntimeException(prompt.exception);
                }
            } else {
                return prompt.result();
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    public void resume(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            Prompt activePrompt = active.get();

            if (activePrompt != null) {
                if (prompt == activePrompt) {
                    throw new IllegalStateException("Prompt " + prompt + " already showing");
                } else {
                    dismiss(activePrompt);
                }
            }

            if (prompt.result() != Prompt.UNDEFINED)
                return;

            prompt.resume();
            makePromptActive(prompt);

            // Were're just resuming, no need to wait or cleanup
        } finally {
            prompt.lock.unlock();
        }
    }

    private void makePromptActive(Prompt prompt) throws InterruptedException {
        active.set(prompt);
        for (InputMethod inputMethod : prompt.inputMethods) {
            inputMethod.show(prompt);
        }
    }

    public boolean dismiss(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (prompt.result() == Prompt.UNDEFINED) {
                prompt.setResultOnce(Prompt.DISMISSED);
                return dismissPrompt(prompt);
            } else {
                return false;
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean pause(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            prompt.pause();
            if (prompt.result() == Prompt.UNDEFINED) {
                return dismissPrompt(prompt);
            } else {
                return false;
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    private boolean dismissPrompt(Prompt prompt) {
        Prompt activePrompt = active.get();

        if (activePrompt == null) {
            throw new IllegalArgumentException("Prompt already paused: " + prompt);
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
