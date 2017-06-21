/**
 * 
 */
package teaselib.core.ui;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;

public class Prompt {
    final Choices choices;
    final Choices derived;
    final ScriptFutureTask scriptTask;
    final Confidence confidence;

    final CyclicBarrier lock;
    boolean paused = false;

    public Prompt(Choices choices, Choices derived,
            ScriptFutureTask scriptFutureTask, Confidence confidence) {
        super();
        this.choices = choices;
        this.derived = derived;
        this.scriptTask = scriptFutureTask;
        this.confidence = confidence;
        this.lock = new CyclicBarrier(Integer.MAX_VALUE);
    }

    void pauseUntilResumed() {
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
        }
    }

    void executeScriptTask() {
        if (scriptTask != null) {
            scriptTask.execute();
        }
    }

    void stopScriptTask() {
        if (scriptTask != null) {
            if (!scriptTask.isDone()) {
                scriptTask.cancel(true);
                scriptTask.join();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    @Override
    public String toString() {
        return (scriptTask != null ? scriptTask.getRelation() + " " : " ") + ""
                + choices.toString() + " "
                + (pauseRequested() && pausing() ? "waiting" : "active");
    }

    public void enterPause() {
        paused = true;
    }

    public boolean pauseRequested() {
        return paused;
    }

    public boolean pausing() {
        return lock.getNumberWaiting() > 0;
    }

    public void resume() {
        paused = false;
        lock.reset();
    }
}