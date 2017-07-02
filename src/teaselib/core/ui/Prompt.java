/**
 * 
 */
package teaselib.core.ui;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

public class Prompt {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    static final int DISMISSED = -1;
    static final int PAUSED = -2;
    static final int UNDEFINED = Integer.MIN_VALUE;

    final Choices choices;
    final Choices derived;
    final ScriptFunction scriptFunction;
    final List<InputMethod> inputMethods;

    ScriptFutureTask scriptTask;

    private final CyclicBarrier lock;
    boolean paused = false;

    public Prompt(Choices choices, Choices derived, ScriptFunction scriptFunction, List<InputMethod> inputMethods) {
        super();
        this.choices = choices;
        this.derived = derived;
        this.scriptFunction = scriptFunction;
        this.inputMethods = inputMethods;

        this.lock = new CyclicBarrier(Integer.MAX_VALUE);
    }

    void pauseUntilResumed() {
        logger.info("pause until resuming " + this);
        try {
            synchronized (this) {
                notifyAll();
            }
            lock.await();
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (BrokenBarrierException e) {
            if (pauseRequested()) {
                throw new IllegalStateException();
            }
        } finally {
        }

        if (pauseRequested()) {
            throw new IllegalStateException("Pause still requested");
        } else if (pausing()) {
            throw new IllegalStateException("Barrier still locked");
        }

        logger.info("resumed - continuing " + this);
    }

    void executeScriptTask(TeaseScriptBase script, final Callable<Boolean> dismiss) {
        scriptTask = new ScriptFutureTask(script, scriptFunction, derived, new ScriptFutureTask.TimeoutClick(),
                dismiss);
        scriptTask.execute();
    }

    void completeScriptTask() {
        if (scriptTask != null) {
            if (!scriptTask.finishing() && !scriptTask.isDone()) {
                scriptTask.cancel(true);
            }
            scriptTask.join();
            try {
                forwardErrorsAsRuntimeException();
            } catch (ScriptInterruptedException e) {
                if (scriptTask.getScriptFunctionResult() != ScriptFunction.Timeout && !scriptTask.timedOut()) {
                    throw e;
                }
            }
        }
    }

    void forwardErrorsAsRuntimeException() {
        if (scriptTask != null) {
            scriptTask.forwardErrorsAsRuntimeException();
        }
    }

    String choice(int resultIndex) {
        String choice = scriptTask != null ? scriptTask.getScriptFunctionResult() : null;
        if (choice == null) {
            if (scriptTask != null && scriptTask.timedOut()) {
                choice = ScriptFunction.Timeout;
            } else if (resultIndex == Prompt.DISMISSED) {
                choice = ScriptFunction.Timeout;
            } else if (resultIndex == UNDEFINED) {
                throw new IllegalStateException("Undefined prompt result");
            } else {
                choice = choices.get(resultIndex);
            }
        }
        return choice;
    }

    @Override
    public String toString() {
        return (scriptTask != null ? scriptTask.getRelation() + " " : " ") + "" + choices.toString() + " "
                + (pauseRequested() && pausing() ? "waiting" : "active");
    }

    void enterPause() {
        paused = true;
    }

    boolean pauseRequested() {
        return paused;
    }

    boolean pausing() {
        return lock.getNumberWaiting() > 0;
    }

    void resume() {
        logger.info("Resuming " + this);
        paused = false;
        lock.reset();
    }
}
