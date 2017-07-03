package teaselib.core.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.ui.PromptQueue.Todo;
import teaselib.core.ui.PromptQueue.Todo.Action;

/**
 * @author Citizen-Cane
 *
 */
public class HostInputMethod implements InputMethod {
    private final Host host;

    ExecutorService workerThread = NamedExecutorService.singleThreadedQueue(getClass().getName());

    private final ReentrantLock replySection = new ReentrantLock(true);

    public HostInputMethod(Host host) {
        super();
        this.host = host;
    }

    @Override
    public void show(final Todo todo) {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                synchronized (HostInputMethod.this) {
                    if (todo.action == Action.Show) {
                        replySection.lockInterruptibly();
                        try {
                            if (todo.result() == Prompt.UNDEFINED) {
                                int reply = host.reply(todo.prompt.derived);
                                if (todo.paused.get() == false) {
                                    todo.setResultOnce(reply);
                                }
                            }
                        } catch (Throwable t) {
                            todo.exception = t;
                        } finally {
                            // TODO find out if needed
                            HostInputMethod.this.notifyAll();
                            synchronized (todo.prompt) {
                                if (todo.paused.get() == false) {
                                    todo.prompt.notifyAll();
                                }
                            }
                            replySection.unlock();
                        }
                    }

                }
                return todo.result();
            }
        };

        workerThread.submit(callable);

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
                synchronized (prompt) {
                    prompt.wait(100);
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            synchronized (prompt) {
                if (replySection.isHeldByCurrentThread()) {
                    replySection.unlock();
                }
            }
        }
        return dismissChoices;
    }

}
