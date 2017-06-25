package teaselib.core.ui;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.PromptQueue.Todo;
import teaselib.core.ui.PromptQueue.Todo.Action;

/**
 * @author Citizen-Cane
 *
 */
public class HostInputMethod implements InputMethod {
    private final Host host;

    Thread workerThread;
    private final ReentrantLock replySection = new ReentrantLock();
    private final SynchronousQueue<Todo> todos = new SynchronousQueue<Todo>();

    public HostInputMethod(Host host) {
        super();
        this.host = host;

        workerThread = new Thread(worker);
        workerThread.setName(getClass().getSimpleName() + "@" + hashCode());
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
                        synchronized (HostInputMethod.this) {
                            if (todo.action == Action.Show) {
                                replySection.lockInterruptibly();
                                try {
                                    final int reply = host.reply(todo.prompt.derived);
                                    if (todo.result == PromptQueue.UNDEFINED) {
                                        todo.result = reply;
                                    }
                                } finally {
                                    notifyWaiters();
                                    todo.prompt.notifyAll();
                                    replySection.unlock();
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Expected
            }
        }
    };

    private void notifyWaiters() {
        notifyAll();
    }

    @Override
    public void show(Todo todo) {
        try {
            todos.put(todo);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
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
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (prompt) {
                replySection.unlock();
                // prompt.notifyAll();
            }
        }
        return dismissChoices;
    }

}
