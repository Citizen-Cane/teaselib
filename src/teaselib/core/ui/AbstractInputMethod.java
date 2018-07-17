package teaselib.core.ui;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    // TODO Move executor to MediaRenderQueue
    protected final NamedExecutorService executor = NamedExecutorService.singleThreadedQueue(getClass().getName());
    protected final ReentrantLock replySection = new ReentrantLock(true);

    private Future<Integer> worker;

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
            int result = awaitResult(prompt);
            signalResult(prompt, result);
        }
        return prompt.result();
    }

    protected abstract int awaitResult(Prompt prompt) throws InterruptedException, ExecutionException;

    private void signalResult(Prompt prompt, int result) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (!prompt.paused() && prompt.result() == Prompt.UNDEFINED) {
                prompt.signalResult(this, result);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    @Override
    public final boolean dismiss(Prompt prompt) throws InterruptedException {
        if (worker.isCancelled() || worker.isDone()) {
            return false;
        }
        worker.cancel(true);

        try {
            return handleDismiss(prompt);
        } finally {
            if (replySection.isHeldByCurrentThread()) {
                replySection.unlock();
            }
            executor.shutdown();
        }
    }

    protected abstract boolean handleDismiss(Prompt prompt) throws InterruptedException;

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }

}
