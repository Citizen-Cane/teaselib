/**
 * 
 */
package teaselib.core.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.PromptPipeline.Todo.Action;

/**
 * @author someone
 *
 */
public class PromptPipeline {
    static final int UNDEFINED = Integer.MIN_VALUE;

    public static class Todo {
        enum Action {
            Show,
            Dismiss
        }

        final Action action;
        final Prompt prompt;
        int result;

        public Todo(Action action, Prompt prompt) {
            super();
            this.action = action;
            this.prompt = prompt;
            this.result = UNDEFINED;
        }

    }

    private final HostInputMethod inputMethod;

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    public PromptPipeline(HostInputMethod inputMethod) {
        this.inputMethod = inputMethod;
    }

    public int show(Prompt prompt) {
        synchronized (prompt) {
            synchronized (dismissedPermanent) {
                if (dismissedPermanent.contains(prompt)) {
                    return Prompt.DISMISSED;
                }
            }

            if (active.get() != null && prompt == active.get().prompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            }

            if (active.get() != null) {
                dismiss(active.get().prompt, Prompt.PAUSED);
            }

            try {
                Todo todo = new Todo(Action.Show, prompt);
                active.set(todo);

                inputMethod.show(todo);
                prompt.wait();

                active.set(null);

                return todo.result;
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    public boolean dismiss(Prompt prompt) {
        if (active.get() == null) {
            return false;
        }

        if (prompt != active.get().prompt) {
            throw new IllegalStateException(
                    "Prompt to dismiss " + prompt + " is not active prompt " + active.get().prompt);
        }

        return dismiss(prompt, Prompt.DISMISSED);
    }

    private boolean dismiss(Prompt prompt, int reason) {
        active.get().result = reason;
        try {
            return inputMethod.dismiss(prompt);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            waitUntilDismissed();
            notifyPausedPrompt(prompt);
        }
    }

    private void waitUntilDismissed() {
        synchronized (inputMethod) {
            active.set(null);
        }
    }

    private void notifyPausedPrompt(Prompt prompt) {
        synchronized (prompt) {
            if (active.get() != null) {
                throw new IllegalStateException("Prompt " + prompt + ": active not cleared");
            }
            prompt.notifyAll();
        }
    }

    public boolean dismissUntilLater(Prompt prompt) {
        synchronized (dismissedPermanent) {
            dismissedPermanent.add(prompt);
        }
        return dismiss(prompt);
    }

    public Prompt getActive() {
        final Todo todo = active.get();
        return todo != null ? todo.prompt : null;
    }
}
