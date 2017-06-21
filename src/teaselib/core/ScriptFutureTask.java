/**
 * 
 */
package teaselib.core;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.events.Delegate;

public class ScriptFutureTask extends FutureTask<String> {
    private static final Logger logger = LoggerFactory
            .getLogger(ScriptFutureTask.class);

    public static class TimeoutClick {
        public boolean clicked = false;
    }

    private final ScriptFunction scriptFunction;
    private final TimeoutClick timeout;

    private Throwable throwable = null;
    private boolean done = false;

    private final static ExecutorService Executor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE,
                    ShowChoices.class.getName() + " Script Function", 1,
                    TimeUnit.HOURS);

    public ScriptFutureTask(final TeaseScriptBase script,
            final ScriptFunction scriptFunction,
            final List<String> derivedChoices, final TimeoutClick timeout) {
        super(new Callable<String>() {
            @Override
            public String call() throws Exception {
                synchronized (scriptFunction) {
                    try {
                        scriptFunction.run();
                        // Keep choices available until the last part of
                        // the script function has finished rendering
                        if (Thread.interrupted()) {
                            throw new ScriptInterruptedException();
                        }
                        script.completeAll();
                        // Ignored
                        return null;
                    } catch (ScriptInterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        clickToFinishFunction(script, derivedChoices, timeout);
                    }
                }
            }

            private void clickToFinishFunction(final TeaseScriptBase script,
                    final List<String> derivedChoices,
                    final TimeoutClick timeout) {
                // Script function finished
                List<Delegate> clickables = script.teaseLib.host
                        .getClickableChoices(derivedChoices);
                if (!clickables.isEmpty()) {
                    Delegate clickable = clickables.get(0);
                    if (clickable != null) {
                        // Signal timeout and click any button
                        timeout.clicked = true;
                        logger.info("Script function finished click");
                        // Click any delegate
                        clickables.get(0).run();
                    } else {
                        // Host implementation is incomplete
                        throw new IllegalStateException(
                                "Host didn't return clickables for choices: "
                                        + derivedChoices.toString());
                    }
                } else {
                    logger.info("Script function dismissed already");
                }
            }
        });
        this.scriptFunction = scriptFunction;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }
    }

    @Override
    protected void setException(Throwable t) {
        throwable = t;
        super.setException(t);
    }

    public synchronized Throwable getException() throws InterruptedException {
        while (!done) {
            wait();
        }
        return throwable;
    }

    public void execute() {
        Executor.execute(this);
    }

    public void join() {
        synchronized (scriptFunction) {
            // Intentionally left blank,
            // we just have to be able to enter the synchronized block
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
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

}
