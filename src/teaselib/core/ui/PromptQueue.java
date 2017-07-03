package teaselib.core.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.PromptQueue.Todo.Action;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {

    public static class Todo {
        enum Action {
            Show,
            Dismiss
        }

        final Action action;
        final Prompt prompt;
        Throwable exception;
        final AtomicBoolean paused = new AtomicBoolean(false);
        private int result;

        public Todo(Action action, Prompt prompt) {
            super();
            this.action = action;
            this.prompt = prompt;
            this.exception = null;
            this.result = Prompt.UNDEFINED;
        }

        public synchronized int result() {
            return result;
        }

        public synchronized void setResultOnce(int value) {
            if (result == Prompt.UNDEFINED) {
                result = value;
            }
        }

        @Override
        public String toString() {
            return action.toString() + " " + prompt + " result=" + toString(result, prompt);
        }

        private static String toString(int result, Prompt prompt) {
            if (result == Prompt.UNDEFINED) {
                return "UNDEFINED";
            } else if (result == Prompt.PAUSED) {
                return "PAUSED";
            }
            if (result == Prompt.DISMISSED) {
                return "DISMISSED";
            } else {
                return prompt.choice(result);
            }
        }

    }

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    private final Map<Prompt, Todo> todos = new HashMap<Prompt, Todo>();

    public int show(Prompt prompt) {
        synchronized (prompt) {
            try {
                synchronized (dismissedPermanent) {
                    if (dismissedPermanent.contains(prompt)) {
                        return Prompt.DISMISSED;
                    }
                }

                if (active.get() != null && prompt == active.get().prompt) {
                    throw new IllegalStateException("Prompt " + prompt + " already showing");
                }

                if (active.get() != null) {
                    // TODO must dismiss to pause state in order to to restore
                    // prompt
                    // but runs into IllegalMonitorException in HostInputMethod
                    dismiss(active.get().prompt, Prompt.PAUSED);
                    // dismiss(active.get().prompt);
                }

                Todo todo = new Todo(Action.Show, prompt);
                active.set(todo);
                todos.put(prompt, todo);
                try {
                    for (InputMethod inputMethod : prompt.inputMethods) {
                        inputMethod.show(todo);
                    }

                    // This waits for the prompt being dismissed
                    // either by input or timeout
                    prompt.wait();
                } finally {
                    for (InputMethod inputMethod : prompt.inputMethods) {
                        inputMethod.dismiss(prompt);
                    }

                    active.set(null);
                }
                if (todo.exception != null) {
                    if (todo.exception instanceof RuntimeException) {
                        throw (RuntimeException) todo.exception;
                    } else {
                        throw new RuntimeException(todo.exception);
                    }
                } else {
                    return todo.result;
                }
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    public void resume(Prompt prompt) {
        // TODO Duplicated and changed code from show() -> try to refactor
        synchronized (prompt) {
            synchronized (dismissedPermanent) {
                if (dismissedPermanent.contains(prompt)) {
                    return;
                }
            }

            if (active.get() != null && prompt == active.get().prompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            }

            if (active.get() != null) {
                // TODO must dismiss to pause state in order to to restore
                // prompt
                // but runs into IllegalMonitorException in HostInputMethod
                // dismiss(active.get().prompt, Prompt.PAUSED);
                dismiss(active.get().prompt);
                // TODO Probably there is, in that case the host could directly
                // switch between them
                // throw new IllegalStateException("Resume assumes there's
                // no active prompt");
            }

            // Todo todo = new Todo(Action.Show, prompt);

            Todo todo = todos.get(prompt);

            if (todo.result != Prompt.UNDEFINED)
                return;

            todo.paused.set(false);
            active.set(todo);
            // todos.put(prompt, todo);
            for (InputMethod inputMethod : prompt.inputMethods) {
                inputMethod.show(todo);
            }
            // Were're just resuming, no need to wait or cleanup
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

    public boolean dismiss(Prompt prompt, int reason) {
        synchronized (prompt) {
            // PAUSED is not a result but we're resuming wit ha new todo anyway
            if (reason == Prompt.PAUSED) {
                todos.get(prompt).paused.set(true);
            } else {
                todos.get(prompt).setResultOnce(reason);
            }

            try {
                boolean dismissed = false;
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
                // notifyPausedPrompt(prompt);
            }
        }
    }

    private void waitUntilDismissed(Prompt prompt) {
        for (InputMethod inputMethod : prompt.inputMethods) {
            synchronized (inputMethod) {
                // TODO check if necessary
            }
        }
        active.set(null);
        if (todos.get(prompt).paused.get() == false) {
            todos.remove(prompt);
        }
    }

    // private void notifyPausedPrompt(Prompt prompt) {
    // synchronized (prompt) {
    // if (active.get() != null) {
    // throw new IllegalStateException("Prompt " + prompt + ": active not
    // cleared");
    // }
    // prompt.notifyAll();
    // }
    // }

    public Prompt getActive() {
        final Todo todo = active.get();
        return todo != null ? todo.prompt : null;
    }

    public Callable<Boolean> getDismissCallable(final Prompt prompt) {
        Callable<Boolean> dismiss = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // TODO Reactivated after
                return dismissUntilLater(prompt);
                // TODO remove method PromptPipeline.dismissUntilLater()
                // return promptPipeline.dismiss(Prompt.this);
            }
        };
        return dismiss;
    }

}
