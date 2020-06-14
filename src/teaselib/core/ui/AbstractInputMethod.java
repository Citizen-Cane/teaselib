package teaselib.core.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    public static final Setup Unused = () -> { //
    };

    protected final ExecutorService executor;
    protected final ReentrantLock replySection = new ReentrantLock(true);
    protected final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    private Callable<Prompt.Result> getResult = new Callable<Prompt.Result>() {
        @Override
        public Prompt.Result call() throws Exception {
            Prompt prompt = activePrompt.get();

            replySection.lock();
            try {
                synchronized (this) {
                    notifyAll();
                }

                prompt.inputMethodInitializers.setup(AbstractInputMethod.this);
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

    private Future<Prompt.Result> result;

    public AbstractInputMethod(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void show(Prompt prompt) throws InterruptedException {
        synchronized (getResult) {
            activePrompt.set(prompt);
            result = executor.submit(getResult);
            while (!replySection.isLocked() && prompt.result().equals(Prompt.Result.UNDEFINED)) {
                getResult.wait();
            }
        }
    }

    Prompt.Result awaitAndSignalResult(Prompt prompt) throws InterruptedException, ExecutionException {
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

    protected <T extends InputMethodEventArgs> boolean signalActivePrompt(Runnable action, T eventArgs) {
        Prompt prompt = activePrompt.get();
        if (prompt != null) {
            if (prompt.paused()) {
                // TODO calling this multiple times in a row is unstable
                // - interjecting empty runnable results in previous interjection also not being called
                // - works in debug mode
                // -> need to wait until previous handler has finished and prompt is established again
                // TODO sometimes throws java.util.NoSuchElementException:
                // "Event teaselib.core.ScriptEvents$ScriptEventAction@1e4d3ce5 in event source 'Before Message'."
                throw new UnsupportedOperationException(
                        "Command queueing for signalActivePrompt(Runnable action, T eventArgs)");
            }
            return signal(prompt, action, eventArgs);
        } else {
            return false;
        }
    }

    public static <T extends InputMethodEventArgs> boolean signal(Prompt prompt, Runnable action, T eventArgs) {
        prompt.when(eventArgs.source).run(e -> {
            prompt.remove(e.source);
            action.run();
        });

        prompt.lock.lock();
        try {
            prompt.signalHandlerInvocation(eventArgs);
        } finally {
            prompt.lock.unlock();
        }

        return true;
    }

    @Override
    public final boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt active = activePrompt.getAndSet(null);
        if (active == null) {
            return false;
        } else if (active != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        }

        if (result.isCancelled()) {
            return false;
        } else if (result.isDone()) {
            try {
                result.get();
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
            return false;
        } else {
            result.cancel(true);
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

}
