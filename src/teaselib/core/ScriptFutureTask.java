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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.concurrency.NamedExecutorService;

public class ScriptFutureTask extends FutureTask<String> {
    private static final Logger logger = LoggerFactory
            .getLogger(ScriptFutureTask.class);

    public static class TimeoutClick {
        public boolean clicked = false;
    }

    private final ScriptFunction scriptFunction;
    private final TimeoutClick timeout;
    private Throwable throwable = null;

    private final Host host;
    private final List<String> derivedChoices;

    private CountDownLatch finished;

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
            }
        });
        this.scriptFunction = scriptFunction;
        this.timeout = timeout;

        this.host = script.teaseLib.host;
        this.derivedChoices = derivedChoices;
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            timeout.clicked = host.dismissChoices(derivedChoices);
            finished.countDown();
        }
    }

    @Override
    protected void setException(Throwable t) {
        throwable = t;
        super.setException(t);
    }

    public Throwable getException() throws InterruptedException {
        finished.await();
        return throwable;
    }

    public void execute() {
        finished = new CountDownLatch(1);
        Executor.execute(this);
    }

    public void join() {
        try {
            finished.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
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
