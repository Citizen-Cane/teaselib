package teaselib.core;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Answer;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.AnswerOverride;
import teaselib.core.debug.CheckPoint;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;

public class ScriptFutureTask extends FutureTask<Answer> {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    private final ScriptFunction scriptFunction;
    private final Prompt prompt;
    private final ExecutorService executor;

    private final AtomicBoolean dismissed = new AtomicBoolean(false);
    private final CountDownLatch cancellationCompletion = new CountDownLatch(1);
    private Throwable throwable = null;

    public ScriptFutureTask(Script script, ScriptFunction scriptFunction, Prompt prompt) {
        super(() -> {
            script.teaseLib.checkPointReached(CheckPoint.ScriptFunction.Started);
            try {
                Answer result = scriptFunction.call();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                script.awaitAllCompleted();
                script.teaseLib.checkPointReached(CheckPoint.ScriptFunction.Finished);
                return result;
            } catch (Throwable t) {
                script.endAll();
                throw t;
            }
        });
        this.scriptFunction = scriptFunction;
        this.prompt = prompt;
        this.executor = script.getScriptFuntionExecutorService();
    }

    @Override
    public void run() {
        try {
            super.run();
            logger.info("Script task {} is finishing", prompt);
            if (!isCancelled()) {
                dismissPromptWithTimeout();
            }
        } catch (Throwable e) {
            handleException(e);
        } finally {
            cancellationCompletion.countDown();
        }
        logger.info("Script task {} finished", prompt);
    }

    private void dismissPromptWithTimeout() {
        prompt.lock.lock();
        try {
            if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
                prompt.resume();
                prompt.setTimedOut();
                prompt.click.signalAll();
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    private void handleException(Throwable e) {
        if (throwable == null) {
            setException(e);
        } else {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        dismissed.set(true);
        logger.info("Script task {} cancelled", prompt);
        try {
            return super.cancel(mayInterruptIfRunning);
        } finally {
            forwardErrorsAsRuntimeException();
        }
    }

    @Override
    protected void setException(Throwable t) {
        if (dismissed.get() && (t instanceof ScriptInterruptedException || t instanceof InterruptedException)) {
            logger.info("Script task {} already dismissed", prompt);
        } else {
            throwable = t;
            logger.info("{}:@{} stored - will be forwarded", t.getClass().getSimpleName(), t.hashCode());
            super.setException(t);
        }
        if (!(t instanceof ScriptInterruptedException || t instanceof InterruptedException
                || t instanceof AnswerOverride)) {
            logger.error(t.getMessage(), t);
        }
    }

    public Throwable getException() {
        return throwable;
    }

    public void execute() {
        logger.info("Execute script task {}", prompt);
        throwable = null;
        executor.execute(this);
    }

    public ScriptFunction.Relation getRelation() {
        return scriptFunction.relation;
    }

    @Override
    public Answer get() throws InterruptedException, ExecutionException {
        try {
            logger.info("Waiting for script task {} to join", prompt);
            Answer result = super.get();
            forwardErrorsAsRuntimeException();
            return result;
        } catch (CancellationException e) {
            awaitCancellationCompleted();
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } finally {
            logger.info("Joined script task {}", prompt);
        }
    }

    public void awaitCompleted() throws InterruptedException {
        try {
            logger.info("Waiting for script task {} to complete", prompt);
            super.get();
            forwardErrorsAsRuntimeException();
        } catch (CancellationException e) {
            awaitCancellationCompleted();
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(e);
        } finally {
            logger.info("Completed script task {}", prompt);
        }
    }

    private void awaitCancellationCompleted() throws InterruptedException {
        cancellationCompletion.await();
    }

    private void forwardErrorsAsRuntimeException() {
        Throwable t = getException();
        if (t != null) {
            throwException(t);
        }
    }

    private void throwException(Throwable t) {
        logger.info("Forwarding script task error of {}", prompt);
        if (t instanceof ScriptInterruptedException e) {
            throw e;
        } else if (t instanceof InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } else if (t instanceof RuntimeException e) {
            throw e;
        } else if (t instanceof Error error) {
            throw error;
        } else {
            throw ExceptionUtil.asRuntimeException(t);
        }
    }
}
