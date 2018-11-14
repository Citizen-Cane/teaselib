package teaselib.core;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;

public class ScriptFutureTask extends FutureTask<String> {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    private final ScriptFunction scriptFunction;
    private final Prompt prompt;
    private final ExecutorService executor;

    private final AtomicBoolean dismissed = new AtomicBoolean(false);
    private final CountDownLatch cancellationCompletion = new CountDownLatch(1);
    private Throwable throwable = null;

    public ScriptFutureTask(Script script, ScriptFunction scriptFunction, Prompt prompt) {
        super(() -> {
            try {
                String result = scriptFunction.call();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                script.completeAll();
                return result;
            } catch (Exception e) {
                script.endAll();
                throw e;
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

            prompt.lock.lockInterruptibly();
            logger.info("Script task {} is finishing", prompt);
            try {
                prompt.click.signalAll();
            } finally {
                prompt.lock.unlock();
            }
        } catch (InterruptedException | ScriptInterruptedException e) {
            Thread.currentThread().interrupt();
            handleScriptTaskInterrupted();
        } catch (Exception e) {
            if (throwable == null) {
                setException(e);
            } else {
                logger.error(e.getMessage(), e);
            }
        } finally {
            cancellationCompletion.countDown();
            logger.info("Script task {} finished", prompt);
        }
    }

    private void handleScriptTaskInterrupted() {
        logger.info("Script task {} interrupted", prompt);
        Thread.interrupted();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        dismissed.set(true);
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
        } else if (t instanceof InterruptedException) {
            throwable = new ScriptInterruptedException((InterruptedException) t);
        } else {
            throwable = t;
            logger.info("{}:@{} stored - will be forwarded", t.getClass().getSimpleName(), t.hashCode());
            super.setException(t);
        }
        if (!(t instanceof ScriptInterruptedException || t instanceof InterruptedException)) {
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
    public String get() throws InterruptedException, ExecutionException {
        try {
            logger.info("Waiting for script task {} to join", prompt);
            String result = super.get();
            forwardErrorsAsRuntimeException();
            return result;
        } catch (CancellationException e) {
            cancellationCompletion.await();
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } finally {
            logger.info("Joined script task {}", prompt);
        }
    }

    private void forwardErrorsAsRuntimeException() {
        Throwable t = getException();
        if (t != null) {
            throwException(t);
        }
    }

    private void throwException(Throwable t) {
        logger.info("Forwarding script task error of {}", prompt);
        if (t instanceof ScriptInterruptedException) {
            throw (ScriptInterruptedException) t;
        } else if (t instanceof InterruptedException) {
            throw new ScriptInterruptedException((InterruptedException) t);
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw ExceptionUtil.asRuntimeException(t);
        }
    }
}
