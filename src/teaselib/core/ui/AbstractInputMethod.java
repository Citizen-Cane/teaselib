package teaselib.core.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.Replay;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractInputMethod implements InputMethod {
    protected final ExecutorService executor;
    protected final ReentrantLock replySection = new ReentrantLock(true);
    protected final AtomicReference<Prompt> activePrompt = new AtomicReference<>();

    private final HashMap<String, Runnable> handlers = new HashMap<>();

    private Future<Prompt.Result> worker;

    public AbstractInputMethod(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public final void show(Prompt prompt) throws InterruptedException {
        activePrompt.set(prompt);
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

    @Override
    public final boolean dismiss(Prompt prompt) throws InterruptedException {
        Prompt active = activePrompt.getAndSet(null);
        if (active == null) {
            return false;
        } else if (active != prompt) {
            throw new IllegalStateException("Trying to dismiss wrong prompt");
        }

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
        return handlers;
    }

    protected class SingleShotHandler {
        final String key;
        final Runnable action;

        public SingleShotHandler(String key, Runnable action) {
            this.key = key;
            this.action = action;
            add(this);
        }

        public void run() {
            try {
                action.run();
            } finally {
                remove(this);
            }
        }
    }

    public void add(SingleShotHandler singleShotHandler) {
        handlers.put(singleShotHandler.key, singleShotHandler.action);
    }

    public void remove(SingleShotHandler singleShotHandler) {
        handlers.remove(singleShotHandler.key);
    }

    protected void signal(SingleShotHandler singleShotHandler) {
        activePrompt.getAndUpdate(prompt -> {
            // TODO Parameterize handler to avoid signaling while script functions are running
            if (prompt != null /* && !prompt.hasScriptFunction() */) {
                prompt.script.endAll();
                Replay previous = prompt.script.getReplay();

                // TODO perform interjection as prompt.script actor - not as creator of script
                // -> interjections must use prompt.script.actor, not the one that created the script
                prompt.lock.lock();
                try {
                    prompt.signalHandlerInvocation(singleShotHandler.key);
                } finally {
                    prompt.lock.unlock();
                }

                // TODO intercept renderMessage/Intertitle & showChoices (like SpeechRecognitionrScriptAdapter)
                // -> use events
                boolean finishedWithPrompt = true;
                if (finishedWithPrompt) {
                    previous.replay(Replay.Position.FromMandatory);
                } else {
                    previous.replay(Replay.Position.End);
                }
            }
            return prompt;
        });
    }

}
