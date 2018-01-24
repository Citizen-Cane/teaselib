package teaselib.core.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.concurrency.NamedExecutorService;

/**
 * @author Citizen-Cane
 *
 */
public class HostInputMethod implements InputMethod {
    public interface Backend {
        int reply(Choices choices);

        boolean dismissChoices(List<Choice> choices);
    }

    private final Backend host;
    private final ExecutorService workerThread = NamedExecutorService.singleThreadedQueue(getClass().getName());
    private final ReentrantLock replySection = new ReentrantLock(true);

    public HostInputMethod(Backend host) {
        this.host = host;
    }

    @Override
    public void show(final Prompt prompt) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                replySection.lockInterruptibly();
                try {
                    synchronized (this) {
                        notifyAll();
                    }
                    if (prompt.result() == Prompt.UNDEFINED) {
                        int result = host.reply(prompt.choices);
                        prompt.lock.lockInterruptibly();
                        try {
                            if (!prompt.paused() && prompt.result() == Prompt.UNDEFINED) {
                                prompt.signalResult(result);
                            }
                        } finally {
                            prompt.lock.unlock();
                        }
                    } else {
                        // Ignored because another input method might have dismissed the prompt
                    }
                } finally {
                    replySection.unlock();
                }
                return prompt.result();
            }
        };

        synchronized (callable) {
            workerThread.submit(callable);
            callable.wait();
        }
    }

    @Override
    public boolean dismiss(Prompt prompt) throws InterruptedException {
        boolean dismissChoices = false;
        try {
            boolean tryLock = replySection.tryLock();
            while (!tryLock) {
                dismissChoices = host.dismissChoices(prompt.choices);
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
        } finally {
            if (replySection.isHeldByCurrentThread()) {
                replySection.unlock();
            }
        }
        return dismissChoices;
    }

    @Override
    public Map<String, Runnable> getHandlers() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return host.toString();
    }
}
