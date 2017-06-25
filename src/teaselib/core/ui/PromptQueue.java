package teaselib.core.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.PromptQueue.Todo.Action;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {
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

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    private final Map<Prompt, Todo> todos = new HashMap<Prompt, Todo>();

    public int show(Prompt prompt) {
        synchronized (prompt) {
            try {
                Todo todo;
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

                todo = new Todo(Action.Show, prompt);
                active.set(todo);
                todos.put(prompt, todo);
                for (InputMethod inputMethod : prompt.inputMethods) {
                    inputMethod.show(todo);
                }
                prompt.wait();

                active.set(null);

                return todo.result;
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    public boolean dismissUntilLater(Prompt prompt) {
        synchronized (prompt) {
            synchronized (dismissedPermanent) {
                dismissedPermanent.add(prompt);
            }
            return dismiss(prompt);
        }
    }

    public boolean dismiss(Prompt prompt) {
        synchronized (prompt) {
            if (!todos.containsKey(prompt)) {
                return false;
            }

            return dismiss(prompt, Prompt.DISMISSED);
        }
    }

    private boolean dismiss(Prompt prompt, int reason) {
        todos.get(prompt).result = reason;

        try {
            boolean dismissed = false;
            // TODO input methods are variant, so they must be remembered
            for (InputMethod inputMethod : prompt.inputMethods) {
                dismissed |= inputMethod.dismiss(prompt);
            }
            return dismissed;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            waitUntilDismissed(prompt);
            notifyPausedPrompt(prompt);
        }
    }

    private void waitUntilDismissed(Prompt prompt) {
        for (InputMethod inputMethod : prompt.inputMethods) {
            synchronized (inputMethod) {
                // TODO check if necessary
            }
        }
        active.set(null);
        todos.remove(prompt);
    }

    private void notifyPausedPrompt(Prompt prompt) {
        synchronized (prompt) {
            if (active.get() != null) {
                throw new IllegalStateException("Prompt " + prompt + ": active not cleared");
            }
            prompt.notifyAll();
        }
    }

    public Prompt getActive() {
        final Todo todo = active.get();
        return todo != null ? todo.prompt : null;
    }
}
