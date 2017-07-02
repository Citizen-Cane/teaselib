/**
 * 
 */
package teaselib.core;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.concurrency.NamedExecutorService;

public class ScriptFutureTask extends FutureTask<String> {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    public static class TimeoutClick {
        public boolean clicked = false;
    }

    private final ScriptFunction scriptFunction;
    private final TimeoutClick timeout;
    private Throwable throwable = null;

    private final List<String> derivedChoices;
    private final Callable<Boolean> dismissChoices;

    private AtomicBoolean dismissed = new AtomicBoolean(false);
    private CountDownLatch finishing;
    private CountDownLatch finished;

    private final static ExecutorService Executor = NamedExecutorService.newFixedThreadPool(Integer.MAX_VALUE,
            "Script Function", 1, TimeUnit.HOURS);

    public ScriptFutureTask(final TeaseScriptBase script, final ScriptFunction scriptFunction,
            final List<String> derivedChoices, final TimeoutClick timeout, Callable<Boolean> dismissChoices) {
        super(new Callable<String>() {
            @Override
            public String call() throws Exception {
                scriptFunction.run();
                // Keep choices available until the last part of
                // the script function has finished rendering
                if (Thread.interrupted()) {
                    throw new ScriptInterruptedException();
                }
                script.completeAll();
                // Ignored
                return null;
            }
        });
        this.scriptFunction = scriptFunction;
        this.timeout = timeout;

        this.dismissChoices = dismissChoices;
        this.derivedChoices = derivedChoices;

    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (ScriptInterruptedException e) {
            // Expected
            logger.info("Script task " + derivedChoices + " interrupted");
        } catch (Throwable t) {
            setException(t);
        } finally {
            try {
                // TODO This can be set much earlier...
                logger.info("Script task " + derivedChoices + " is finishing");
                finishing.countDown();
                timeout.clicked = dismissChoices.call();
            } catch (ScriptInterruptedException e) {
                // Expected
                logger.info("Script task " + derivedChoices + " interrupted");
            } catch (Exception e) {
                if (throwable == null) {
                    setException(e);
                } else {
                    logger.error(e.getMessage(), e);
                }
            } finally {
                finished.countDown();
                logger.info("Script task " + derivedChoices + " finished");
            }
        }
    }

    public boolean finishing() {
        return finished.getCount() == 0;
    }

    public boolean finished() {
        return finished.getCount() == 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        dismissed.set(true);
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected void setException(Throwable t) {
        if (dismissed.get() && t instanceof ScriptInterruptedException) {
            logger.info("Script task " + derivedChoices + " already dismissed");
        } else {
            throwable = t;
            logger.info(t.getClass().getSimpleName() + ":@" + t.hashCode() + " stored - will be forwarded");
            super.setException(t);
        }
        if (!(t instanceof ScriptInterruptedException || t instanceof InterruptedException)) {
            logger.error(t.getMessage(), t);
        }
    }

    public Throwable getException() throws InterruptedException {
        finished.await();
        return throwable;
    }

    public void execute() {
        logger.info("Execute script task " + derivedChoices);
        finishing = new CountDownLatch(1);
        finished = new CountDownLatch(1);
        Executor.execute(this);
    }

    public void join() {
        try {
            logger.info("Waiting for script task " + derivedChoices + " to join");
            finishing.await();
            finished.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            logger.info("Joined script task " + derivedChoices);
        }
    }

    public boolean timedOut() {
        return timeout.clicked;
    }

    public ScriptFunction.Relation getRelation() {
        return scriptFunction.relation;
    }

    public String getScriptFunctionResult() {
        return scriptFunction.result;
    }

    public void forwardErrorsAsRuntimeException() {
        try {
            Throwable t = getException();
            if (t != null) {
                logger.info("Forwarding script task error of " + derivedChoices);
                if (t instanceof ScriptInterruptedException) {
                    throw (ScriptInterruptedException) t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }
}
