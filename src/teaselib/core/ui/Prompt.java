/**
 * 
 */
package teaselib.core.ui;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.ScriptFunction;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;

public class Prompt {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFutureTask.class);

    static final int DISMISSED = -1;

    static final int PAUSED = -2;

    final Choices choices;
    final Choices derived;
    final ScriptFunction scriptFunction;
    ScriptFutureTask scriptTask;
    final Confidence confidence;

    final CyclicBarrier lock;
    boolean paused = false;

    public Prompt(Choices choices, Choices derived, ScriptFunction scriptFunction, Confidence confidence) {
        super();
        this.choices = choices;
        this.derived = derived;
        this.scriptFunction = scriptFunction;
        this.confidence = confidence;
        this.lock = new CyclicBarrier(Integer.MAX_VALUE);
    }

    void pauseUntilResumed() {
        logger.info("pause until resuming " + this);
        try {
            synchronized (this) {
                notifyAll();
                // TODO the lock should be part of the synchronized block,
                // to ensure that notified listeners find the prompt waiting,
                // and can continue execution immediately instead of trying
                // again.
                // But that would cause a deadlock because notified threads
                // would only continue execution after we've left the
                // synchronized block.
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

    void executeScriptTask(TeaseScriptBase script, final PromptPipeline promptPipeline) {
        if (scriptFunction != null) {
            Callable<Boolean> dismiss = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return promptPipeline.dismissUntilLater(Prompt.this);
                }
            };

            scriptTask = new ScriptFutureTask(script, scriptFunction, derived, new ScriptFutureTask.TimeoutClick(),
                    dismiss);

            scriptTask.execute();
        }
    }

    void completeScriptTask() {
        if (scriptTask != null) {
            if (!scriptTask.isDone()) {
                scriptTask.cancel(true);
            }
            scriptTask.join();
            forwardErrorsAsRuntimeException();
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
            // TODO SR
            if (scriptTask != null && scriptTask.timedOut()) {
                // Timeout
                choice = ScriptFunction.Timeout;
            } else if (resultIndex == Prompt.DISMISSED) {
                choice = ScriptFunction.Timeout;
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