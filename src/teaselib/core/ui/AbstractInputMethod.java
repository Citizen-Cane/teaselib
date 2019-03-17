package teaselib.core.ui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    protected final ExecutorService executor;
    protected final ReentrantLock replySection = new ReentrantLock(true);

    private Future<Prompt.Result> worker;

    public AbstractInputMethod(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void show(Prompt prompt) throws InterruptedException {
        Callable<Prompt.Result> callable = new Callable<Prompt.Result>() {
            @Override
            public Prompt.Result call() throws Exception {
                replySection.lock();
                try {
                    synchronized (this) {
                        notifyAll();
                    }

                    return awaitAndSignalResult(prompt);
                } catch (InterruptedException | ScriptInterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    prompt.setException(e);
                    throw e;
                } finally {
                    replySection.unlock();
                }
            }
        };

        synchronized (callable) {
            worker = executor.submit(callable);
            while (!replySection.isLocked() && prompt.result().equals(Prompt.Result.UNDEFINED)) {
                callable.wait();
            }
        }
    }

    private Prompt.Result awaitAndSignalResult(Prompt prompt) throws InterruptedException, ExecutionException {
        if (prompt.result() == Prompt.Result.UNDEFINED) {
            Prompt.Result result = handleShow(prompt);
            signalResult(prompt, result);
        }
        return prompt.result();
    }

    protected abstract Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException;

    private void signalResult(Prompt prompt, Prompt.Result result) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (!prompt.paused()) {
                prompt.signalResult(this, result);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public final boolean dismiss(Prompt prompt) throws InterruptedException {
        if (worker.isCancelled()) {
            return false;
        } else if (worker.isDone()) {
            try {
                worker.get();
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
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
            }

            return dismissChoices;
        } catch (InterruptedException | ScriptInterruptedException e) {
            throw e;
        } catch (Throwable e) {
            prompt.setException(e);
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
