/**
 * 
 */
package teaselib.core;

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
import teaselib.core.ui.Prompt;

public class ScriptFutureTask extends FutureTask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    private final ScriptFunction scriptFunction;
    private final AtomicBoolean timedOut = new AtomicBoolean(false);
    private Throwable throwable = null;

    private final Prompt prompt;
    private final Callable<Boolean> dismissChoices;

    private AtomicBoolean dismissed = new AtomicBoolean(false);
    private CountDownLatch finishing;
    private CountDownLatch finished;

    private final static ExecutorService Executor = NamedExecutorService.newFixedThreadPool(Integer.MAX_VALUE,
            "Script Function", 1, TimeUnit.HOURS);

    public ScriptFutureTask(final TeaseScriptBase script, final ScriptFunction scriptFunction, Prompt prompt,
            Callable<Boolean> dismissChoices) {
        super(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
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
                    script.endAll();
                    throw e;
                }
            }
        });
        this.scriptFunction = scriptFunction;
        this.prompt = prompt;
        this.dismissChoices = dismissChoices;
    }

    @Override
    public void run() {
        try {
            // A good sleep makes most tests succeed, add sync here
            // TODO Wait until prompt queue starts waiting for the signal condition
            // - this is just before the prompt is realized
            prompt.lock.lockInterruptibly();
            try {
                // Wait until prompt queue is ready to be signaled
            } finally {
                prompt.lock.unlock();
            }

            super.run();
        } catch (ScriptInterruptedException e) {
            // Expected
            logger.info("Script task " + prompt + " interrupted");
        } catch (Throwable t) {
            setException(t);
        } finally {
            try {
                // TODO This can be set much earlier...
                logger.info("Script task " + prompt + " is finishing");
                finishing.countDown();

                // Resolved failing tests with debug setup regarding "Timeout instead of reply:stop"
                // - the test is always correct since the script function is just so fast that there is no time to click
                // TODO ensure input method can click on stop before the script function times out (it's just too fast)
                // Thread.sleep(1000);

                // Better, but of course not perfect
                // Thread.yield();

                // resume parent thread
                prompt.lock.lockInterruptibly();
                try {
                    timedOut.set(prompt.result() == Prompt.UNDEFINED);
                    prompt.click.signalAll();
                } finally {
                    prompt.lock.unlock();
                }

            } catch (InterruptedException e) {
                // Ignore interrupt while dismissing prompt since the script function is finishing already,
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
                finished.countDown();
                logger.info("Script task " + prompt + " finished");
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

    public Throwable getException() throws InterruptedException {
        finished.await();
        return throwable;
    }

    public void execute() {
        logger.info("Execute script task " + prompt);
        finishing = new CountDownLatch(1);
        finished = new CountDownLatch(1);
        Executor.execute(this);
    }

    public void join() {
        try {
            logger.info("Waiting for script task " + prompt + " to join");
            finishing.await();
            finished.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
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
        try {
            Throwable t = getException();
            if (t != null) {
                throwException(t);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
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
