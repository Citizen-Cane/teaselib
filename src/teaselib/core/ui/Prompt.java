/**
 * 
 */
package teaselib.core.ui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.ScriptFunction;
import teaselib.core.ScriptFutureTask;
import teaselib.core.TeaseScriptBase;

public class Prompt {
    static final int DISMISSED = -1;
    static final int UNDEFINED = Integer.MIN_VALUE;

    static final String NONE = "None";

    final Choices choices;
    final Choices derived;
    final ScriptFunction scriptFunction;
    final List<InputMethod> inputMethods;

    ScriptFutureTask scriptTask;

    final ReentrantLock lock;
    final Condition click;

    String inputHandlerKey = NONE;

    public Prompt(Choices choices, Choices derived, ScriptFunction scriptFunction, List<InputMethod> inputMethods) {
        super();
        this.choices = choices;
        this.derived = derived;
        this.scriptFunction = scriptFunction;

        this.inputMethods = inputMethods;
        this.lock = new ReentrantLock();
        this.click = lock.newCondition();
    }

    void executeScriptTask(TeaseScriptBase script, final Callable<Boolean> dismiss) {
        scriptTask = new ScriptFutureTask(script, scriptFunction, derived, new ScriptFutureTask.TimeoutClick(),
                dismiss);
        scriptTask.execute();
    }

    void dismissScriptTask() {
        if (scriptTask != null) {
            scriptTask.join();
        }
    }

    void completeScriptTask() {
        if (scriptTask != null) {
            if (!scriptTask.finishing() && !scriptTask.isDone()) {
                scriptTask.cancel(true);
            }
            scriptTask.join();
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

    void executeInputMethodHandler() {
        String key = inputHandlerKey;
        for (InputMethod inputMethod : this.inputMethods) {
            Map<String, Runnable> handlers = inputMethod.getHandlers();
            if (handlers.containsKey(key)) {
                handlers.get(key).run();
                return;
            }
        }
        throw new IllegalArgumentException("No handler for " + key);
    }

    @Override
    public String toString() {
        return (scriptTask != null ? scriptTask.getRelation() + " " : " ") + "" + choices.toString() + " "
                + (lock.hasWaiters(click) ? "waiting" : "active");
    }
}
