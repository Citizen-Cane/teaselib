package teaselib.core;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;

public class ScriptFutureTask extends FutureTask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    private static final ExecutorService Executor = NamedExecutorService.newFixedThreadPool(Integer.MAX_VALUE,
            "Script Function", 1, TimeUnit.HOURS);

    private final ScriptFunction scriptFunction;
    private final AtomicBoolean timedOut = new AtomicBoolean(false);
    private final Prompt prompt;

    private AtomicBoolean dismissed = new AtomicBoolean(false);
    private Throwable throwable = null;

    public ScriptFutureTask(Script script, ScriptFunction scriptFunction, Prompt prompt) {
        super(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    scriptFunction.run();
                    if (Thread.interrupted()) {
                        throw new ScriptInterruptedException();
                    }
                    script.completeAll();
                    return null;
                } catch (Exception e) {
                    script.endAll();
                    throw e;
                }
            }
        });
        this.scriptFunction = scriptFunction;
        this.prompt = prompt;
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (ScriptInterruptedException e) {
            logger.info("Script task " + prompt + " interrupted");
        } catch (Throwable t) {
            setException(t);
        }

        try {
            logger.info("Script task " + prompt + " is finishing");
            prompt.lock.lockInterruptibly();
            try {
                timedOut.set(prompt.result() == Prompt.UNDEFINED);
                prompt.click.signalAll();
            } finally {
                prompt.lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Ignore interrupt while dismissing prompt since the script function is finishing,
            // and either has been stopped already or will be dismissed (all good states)
        } catch (ScriptInterruptedException e) {
            // Expected
            logger.info("Script task " + prompt + " interrupted");
        } catch (Exception e) {
            if (throwable == null) {
                setException(e);
            } else {
                logger.error(e.getMessage(), e);
            }
        } finally {
            logger.info("Script task " + prompt + " finished");
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        dismissed.set(true);
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected void setException(Throwable t) {
        if (dismissed.get() && t instanceof ScriptInterruptedException) {
            logger.info("Script task " + prompt + " already dismissed");
        } else {
            throwable = t;
            logger.info(t.getClass().getSimpleName() + ":@" + t.hashCode() + " stored - will be forwarded");
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
        logger.info("Execute script task " + prompt);
        Executor.execute(this);
    }

    public void join() {
        try {
            logger.info("Waiting for script task " + prompt + " to join");
            get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (CancellationException e) {
            // Ignore
        } catch (ExecutionException e) {
            throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
        } finally {
            logger.info("Joined script task " + prompt);
        }
    }

    public boolean timedOut() {
        return timedOut.get();
    }

    public ScriptFunction.Relation getRelation() {
        return scriptFunction.relation;
    }

    public String getScriptFunctionResult() {
        return scriptFunction.result;
    }

    public void forwardErrorsAsRuntimeException() {
        Throwable t = getException();
        if (t != null) {
            throwException(t);
        }
    }

    private void throwException(Throwable t) {
        logger.info("Forwarding script task error of " + prompt);
        if (t instanceof ScriptInterruptedException) {
            throw (ScriptInterruptedException) t;
        } else if (t instanceof InterruptedException) {
            throw new ScriptInterruptedException((InterruptedException) t);
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException(t);
        }
    }
}
