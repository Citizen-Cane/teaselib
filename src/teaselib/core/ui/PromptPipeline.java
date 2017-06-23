/**
 * 
 */
package teaselib.core.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.PromptPipeline.Todo.Action;

/**
 * @author someone
 *
 */
public class PromptPipeline {
    static final int UNDEFINED = Integer.MIN_VALUE;

    private final Host host;

    static class Todo {
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

    SynchronousQueue<Todo> todos = new SynchronousQueue<Todo>();

    Thread workerThread;

    private final AtomicReference<Todo> active = new AtomicReference<Todo>();
    private final ReentrantLock replySection = new ReentrantLock();
    private final Set<Prompt> dismissedPermanent = new HashSet<Prompt>();

    public PromptPipeline(Host host) {
        this.host = host;

        workerThread = new Thread(worker);
        workerThread.setName(PromptPipeline.class.getSimpleName() + "@" + PromptPipeline.this.hashCode());
        workerThread.setDaemon(true);
        workerThread.start();
    }

    Runnable worker = new Runnable() {
        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Todo todo = todos.take();
                    synchronized (todo.prompt) {
                        if (todo.action == Action.Show) {
                            replySection.lockInterruptibly();
                            active.set(todo);
                            try {
                                final int reply = host.reply(todo.prompt.derived);
                                if (todo.result == UNDEFINED) {
                                    todo.result = reply;
                                }
                            } finally {
                                active.set(null);
                                todo.prompt.notifyAll();
                                replySection.unlock();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Expected
            }
        }
    };

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
                todos.put(todo);
                prompt.wait();
                return todo.result;
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    public boolean dismiss(Prompt prompt) {
        if (active.get() == null)
            return false;

        if (prompt != active.get().prompt) {
            throw new IllegalStateException(
                    "Prompt to dismiss " + prompt + " is not active prompt " + active.get().prompt);
        }

        return dismiss(prompt, Prompt.DISMISSED);
    }

    private boolean dismiss(Prompt prompt, int reason) {
        active.get().result = reason;
        boolean dismissChoices = false;
        try {
            boolean tryLock = replySection.tryLock();
            while (!tryLock) {
                dismissChoices = host.dismissChoices(prompt.derived);
                tryLock = replySection.tryLock();
                if (tryLock) {
                    break;
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (active.get() != null) {
                throw new IllegalStateException("Prompt " + prompt + ": active not cleared");
            }
            synchronized (prompt) {
                replySection.unlock();
                prompt.notifyAll();
            }
        }
        return dismissChoices;
    }

    public boolean dismissUntilLater(Prompt prompt) {
        synchronized (dismissedPermanent) {
            dismissedPermanent.add(prompt);
        }
        return dismiss(prompt);
    }

    /**
     * @return
     */
    public Prompt getActive() {
        final Todo todo = active.get();
        return todo != null ? todo.prompt : null;
    }
}
