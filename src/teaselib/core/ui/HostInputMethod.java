package teaselib.core.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.ui.PromptQueue.Todo;

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
                    replySection.lockInterruptibly();
                    Prompt prompt = todo.prompt;
                    try {
                        if (todo.result() == Prompt.UNDEFINED) {
                            int reply = host.reply(prompt.derived);
                            if (todo.paused.get() == false) {
                                todo.setResultOnce(reply);
                            }
                        }
                    } catch (Throwable t) {
                        todo.exception = t;
                    } finally {
                        // TODO find out if needed
                        HostInputMethod.this.notifyAll();
                        prompt.lock.lockInterruptibly();
                        try {
                            if (todo.paused.get() == false) {
                                prompt.click.signalAll();
                            }
                        } finally {
                            prompt.lock.unlock();
                        }
                        replySection.unlock();
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
                prompt.lock.lockInterruptibly();
                try {
                    prompt.click.await(100, TimeUnit.MILLISECONDS);
                } finally {
                    prompt.lock.unlock();
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
