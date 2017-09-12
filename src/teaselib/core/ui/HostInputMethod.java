package teaselib.core.ui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.Host;
import teaselib.core.concurrency.NamedExecutorService;

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
    public void show(final Prompt prompt) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                synchronized (HostInputMethod.this) {
                    replySection.lockInterruptibly();
                    try {
                        synchronized (this) {
                            notifyAll();
                        }
                        if (prompt.result() == Prompt.UNDEFINED) {
                            int reply = host.reply(prompt.derived);
                            if (prompt.paused.get() == false) {
                                prompt.setResultOnce(reply);
                            }
                        } else if (prompt.result() == Prompt.DISMISSED) {
                            // Mainly in debug, without script output - just ignore
                            // logger.warn("prompt " + prompt + " already dismissed");
                            // Here's a small loop hole, since there is no sync on the prompt
                            // - cannot be here since we need to call reply on the host and wait there
                            // throw new IllegalStateException("Prompt " + prompt + " has been dismissed already");
                        } else {
                            throw new IllegalStateException(
                                    "Prompt " + prompt + ": result already set to " + prompt.result());
                        }
                    } catch (Throwable t) {
                        prompt.exception = t;
                    } finally {
                        prompt.lock.lockInterruptibly();
                        try {
                            if (prompt.paused.get() == false) {
                                prompt.click.signalAll();
                            } else {
                                throw new IllegalStateException("Prompt click not signaled for " + prompt);
                            }
                        } finally {
                            prompt.lock.unlock();
                            replySection.unlock();
                        }
                    }
                }
                return prompt.result();
            }
        };

        synchronized (callable) {
            workerThread.submit(callable);
            callable.wait();

            // Failing tests in debug setup : Give the host input method some time to initialize
            // + seems to decrease probability of failures in "testSingleScriptFunction":
            // - hang (nothing to dismiss but replySection seems to be locked)
            // - failing test in debug setup: ScriptInterruptedException when script function is finishing already
            // - failing test in debug setup: org.junit.ComparisonFailure: expected:<[Stop]> but was:<[Timeout]>

            // TODO replace with multi-threading solution
            // Thread.yield();
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

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public String toString() {
        return host.toString();
    }

}
