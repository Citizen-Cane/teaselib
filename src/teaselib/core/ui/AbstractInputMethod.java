package teaselib.core.ui;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.ui.Prompt.Action;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    public static final Setup Unused = () -> { //
    };

    protected final ExecutorService executor;
    protected final ReentrantLock showLock = new ReentrantLock(true);

    protected final ReentrantLock startLock = new ReentrantLock(true);
    private final Condition start = startLock.newCondition();

    protected final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    private Consumer<Prompt> showPrompt = prompt -> {
        showLock.lock();
        try {
            prompt.inputMethodInitializers.setup(AbstractInputMethod.this);

            startLock.lock();
            try {
                start.signalAll();
            } finally {
                startLock.unlock();
            }

            awaitAndSignalResult(prompt);
        } catch (InterruptedException | ScriptInterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            prompt.setException(e);
        } finally {
            showLock.unlock();
        }
    };

    private Future<?> resultWorker;

    protected AbstractInputMethod(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void show(Prompt prompt) throws InterruptedException {
        Objects.requireNonNull(prompt);
        if (showLock.isLocked()) {
            throw new ConcurrentModificationException("Show prompt " + this);
        }

        activePrompt.set(prompt);
        start(prompt);

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private void start(Prompt prompt) throws InterruptedException {
        if (startLock.tryLock()) {
            try {
                resultWorker = executor.submit(() -> showPrompt.accept(prompt));
                start.await();
            } finally {
                startLock.unlock();
            }
        } else {
            throw new IllegalStateException("ReplySection already locked");
        }
    }

    Prompt.Result awaitAndSignalResult(Prompt prompt) throws InterruptedException, ExecutionException {
        if (prompt.unsynchronizedResult() == Prompt.Result.UNDEFINED) {
            var result = handleShow(prompt);
            signal(prompt, result);
        }
        return prompt.unsynchronizedResult();
    }

    protected abstract Prompt.Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException;

    private void signal(Prompt prompt, Prompt.Result result) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            if (prompt.paused()) {
                throw new UnsupportedOperationException(
                        "Trying to signal result " + result + " to paused prompt " + prompt);
            } else if (prompt.result().equals(Prompt.Result.UNDEFINED) && !prompt.paused()) {
                prompt.signal(this, result);
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    protected <T extends InputMethodEventArgs> boolean signalActivePrompt(Runnable action, T eventArgs) {
        var prompt = activePrompt.get();
        if (prompt != null) {
            prompt.lock.lock();
            try {
                if (prompt.paused()) {
                    // TODO calling this multiple times in a row is unstable
                    // - interjecting empty runnable results in previous interjection also not being called
                    // - works in debug mode
                    // -> need to wait until previous handler has finished and prompt is established again
                    // TODO sometimes throws java.util.NoSuchElementException:
                    // "Event teaselib.core.ScriptEvents$ScriptEventAction@1e4d3ce5 in event source 'Before Message'."
                    throw new UnsupportedOperationException(
                            "Command queueing for signalActivePrompt(Runnable action, T eventArgs)");
                } else {
                    return signal(prompt, action, eventArgs);
                }
            } finally {
                prompt.lock.unlock();
            }
        } else {
            return false;
        }
    }

    public static <T extends InputMethodEventArgs> boolean signal(Prompt prompt, Runnable action, T eventArgs) {
        Action runOnceAndRemove = new Action() {
            @Override
            public boolean canRun(InputMethodEventArgs eventArgs) {
                return true;
            }

            @Override
            public void run(InputMethodEventArgs e) {
                prompt.remove(e.source);
                action.run();
            }
        };
        prompt.when(eventArgs.source).then(runOnceAndRemove);

        prompt.lock.lock();
        try {
            prompt.signal(eventArgs);
        } finally {
            prompt.lock.unlock();
        }

        return true;
    }

    @Override
    public abstract void updateUI(UiEvent event);

    @Override
    public final void dismiss(Prompt prompt) {
        activePrompt.updateAndGet(active -> {
            if (active != null && active != prompt) {
                throw new IllegalStateException("Trying to dismiss wrong prompt");
            }

            if (resultWorker.isDone()) {
                try {
                    resultWorker.get();
                } catch (InterruptedException | ScriptInterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
                }
            } else if (!resultWorker.isCancelled()) {
                resultWorker.cancel(true);
            }
            resultWorker = null;

            try {
                boolean tryLock = showLock.tryLock();
                while (!tryLock) {
                    handleDismiss(prompt);
                    tryLock = showLock.tryLock();
                    if (tryLock) {
                        break;
                    }
                }
            } catch (InterruptedException | ScriptInterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                prompt.setException(e);
            } finally {
                if (showLock.isHeldByCurrentThread()) {
                    showLock.unlock();
                }
            }

            // TODO should be null but returning "prompt" helps to fix debug response
            return null;
        });
    }

    protected abstract void handleDismiss(Prompt prompt) throws InterruptedException;

    @Override
    public void close() {
        Future<?> result = resultWorker;
        activePrompt.updateAndGet(prompt -> {
            if (result != null && !result.isDone() && !result.isCancelled()) {
                resultWorker.cancel(true);
            }
            return prompt;
        });
    }

}
