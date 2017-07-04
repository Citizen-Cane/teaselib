package teaselib.core.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ScriptInterruptedException;

/**
 * @author Citizen-Cane
 *
 */
public class PromptQueue {

    public static class Todo {
        final Prompt prompt;
        final AtomicBoolean paused = new AtomicBoolean(false);
        Throwable exception;
        private int result;

        public Todo(Prompt prompt) {
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
            return prompt + " result=" + toString(result, prompt)
                    + (exception != null ? " " + exception.getMessage() : "");
        }

        private static String toString(int result, Prompt prompt) {
            if (result == Prompt.UNDEFINED) {
                return "UNDEFINED";
            } else if (result == Prompt.DISMISSED) {
                return "DISMISSED";
            } else {
                return prompt.choice(result);
            }
        }

    }

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();
    private final Map<Prompt, Todo> todos = new HashMap<Prompt, Todo>();

    // TODO never cleared but have to
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    public int show(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
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
                pause(active.get().prompt);
            }

            Todo todo = new Todo(prompt);
            active.set(todo);
            todos.put(prompt, todo);
            try {
                for (InputMethod inputMethod : prompt.inputMethods) {
                    inputMethod.show(todo);
                }

                prompt.click.await();
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
        } finally {
            prompt.lock.unlock();
        }
    }

    public void resume(Prompt prompt) throws InterruptedException {
        // TODO Duplicated and changed code from show() -> try to refactor
        prompt.lock.lockInterruptibly();
        try {
            synchronized (dismissedPermanent) {
                if (dismissedPermanent.contains(prompt)) {
                    return;
                }
            }

            if (active.get() != null && prompt == active.get().prompt) {
                throw new IllegalStateException("Prompt " + prompt + " already showing");
            }

            if (active.get() != null) {
                dismiss(active.get().prompt);
            }

            Todo todo = todos.get(prompt);

            if (todo.result != Prompt.UNDEFINED)
                return;

            todo.paused.set(false);
            active.set(todo);
            for (InputMethod inputMethod : prompt.inputMethods) {
                inputMethod.show(todo);
            }
            // Were're just resuming, no need to wait or cleanup
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean dismissUntilLater(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            synchronized (dismissedPermanent) {
                dismissedPermanent.add(prompt);
            }
            return dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean dismiss(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (!todos.containsKey(prompt)) {
                return false;
            }

            Todo todo = todos.get(prompt);
            todo.setResultOnce(Prompt.DISMISSED);
            return dismiss(todo);
        } finally {
            prompt.lock.unlock();
        }
    }

    public boolean pause(Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            Todo todo = todos.get(prompt);
            todo.paused.set(true);
            return dismiss(todo);
        } finally {
            prompt.lock.unlock();
        }
    }

    private boolean dismiss(Todo todo) {
        Prompt prompt = todo.prompt;
        try {
            boolean dismissed = false;
            for (InputMethod inputMethod : prompt.inputMethods) {
                dismissed |= inputMethod.dismiss(prompt);
            }
            waitUntilDismissed(prompt);
            return dismissed;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public Prompt getActive() {
        final Todo todo = active.get();
        return todo != null ? todo.prompt : null;
    }

    public Callable<Boolean> getDismissCallable(final Prompt prompt) {
        Callable<Boolean> dismiss = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return dismissUntilLater(prompt);
                // TODO remove method PromptPipeline.dismissUntilLater()
                // return promptPipeline.dismiss(Prompt.this);
            }
        };
        return dismiss;
    }

    public void clear(Prompt prompt) {
        dismissedPermanent.remove(prompt);
    }
}
