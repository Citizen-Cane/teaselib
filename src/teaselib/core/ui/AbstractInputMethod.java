package teaselib.core.ui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    private static final Logger logger = LoggerFactory.getLogger(AbstractInputMethod.class);

    protected final ExecutorService executor;
    protected final ReentrantLock replySection = new ReentrantLock(true);

    private Future<Integer> worker;

    public AbstractInputMethod(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void show(Prompt prompt) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                replySection.lock();
                try {
                    synchronized (this) {
                        notifyAll();
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    return awaitAndSignalResult(prompt);
                } catch (InterruptedException | ScriptInterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    throw e;
                    // TODO Store error in prompt and check after click.await() returns
                } finally {
                    replySection.unlock();
                }
            }
        };

        synchronized (callable) {
            worker = executor.submit(callable);
            while (!replySection.isLocked() && prompt.result() == Prompt.UNDEFINED) {
                callable.wait();
            }
        }
    }

    private Integer awaitAndSignalResult(Prompt prompt) throws InterruptedException, ExecutionException {
        if (prompt.result() == Prompt.UNDEFINED) {
            int result = handleShow(prompt);
            signalResult(prompt, result);
        }
        return prompt.result();
    }

    protected abstract int handleShow(Prompt prompt) throws InterruptedException, ExecutionException;

    private void signalResult(Prompt prompt, int result) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (!prompt.paused() /* && prompt.result() == Prompt.UNDEFINED */) {
                prompt.signalResult(this, result);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public final boolean dismiss(Prompt prompt) throws InterruptedException {
        if (worker.isDone()) {
            try {
                worker.get();
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
            return false;
        } else if (worker.isCancelled()) {
            return false;
        } else {
            worker.cancel(true);
        }

        try {
            boolean dismissChoices = false;

            boolean tryLock = replySection.tryLock();
            while (!tryLock) {
                dismissChoices = handleDismiss(prompt);
                tryLock = replySection.tryLock();
                if (tryLock) {
                    break;
                }
                prompt.lock.lockInterruptibly();
                try {
                    tryLock = prompt.click.await(100, TimeUnit.MILLISECONDS);
                } finally {
                    prompt.lock.unlock();
                }
            }

            return dismissChoices;
        } catch (InterruptedException | ScriptInterruptedException e) {
            throw e;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (replySection.isHeldByCurrentThread()) {
                replySection.unlock();
            }
        }
    }

    protected abstract boolean handleDismiss(Prompt prompt) throws InterruptedException;

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }
}
