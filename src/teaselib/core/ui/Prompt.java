/**
 * 
 */
package teaselib.core.ui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import teaselib.ScriptFunction;
import teaselib.core.Script;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

public class Prompt {
    public static final int DISMISSED = -1;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    static final String NONE = "None";

    public final Choices choices;
    public final Choices derived;
    final ScriptFunction scriptFunction;
    final List<InputMethod> inputMethods;

    ScriptFutureTask scriptTask;

    public final ReentrantLock lock;
    public final Condition click;

    final AtomicBoolean paused = new AtomicBoolean(false);

    private int result;

    String inputHandlerKey = NONE;

    public Prompt(Choices choices, Choices derived, ScriptFunction scriptFunction, List<InputMethod> inputMethods) {
        super();
        this.choices = choices;
        this.derived = derived;
        this.scriptFunction = scriptFunction;

        this.inputMethods = inputMethods;
        this.lock = new ReentrantLock();
        this.click = lock.newCondition();

        this.result = Prompt.UNDEFINED;
    }

    void executeScriptTask(Script script) {
        scriptTask = new ScriptFutureTask(script, scriptFunction, this);
        scriptTask.execute();
    }

    void joinScriptTask() {
        if (scriptTask != null) {
            scriptTask.join();
        }
    }

    void cancelScriptTask() {
        if (scriptTask != null) {
            if (!scriptTask.isCancelled() && !scriptTask.isDone()) {
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
                throw new IllegalStateException("Undefined prompt result for " + this);
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
        throw new IllegalArgumentException("No handler for " + key + " in " + this);
    }

    public synchronized int result() {
        return result;
    }

    public synchronized void setResultOnce(int value) {
        if (result == Prompt.UNDEFINED) {
            result = value;
        } else {
            throw new IllegalStateException("Prompt result can be set only once for " + this);
        }
    }

    public void signalResult(int resultIndex) {
        setResultOnce(resultIndex);

        if (!paused()) {
            click.signalAll();
        } else {
            throw new IllegalStateException("Prompt click signaled for paused prompt " + this);
        }
    }

    public void signalHandlerInvocation(String handlerKey) {
        pause();
        inputHandlerKey = handlerKey;
        click.signalAll();
    }

    public void pause() {
        paused.set(true);
    }

    public boolean paused() {
        return paused.get();
    }

    public void resume() {
        paused.set(false);
    }

    public boolean dismiss() {
        try {
            boolean dismissed = false;
            for (InputMethod inputMethod : inputMethods) {
                dismissed &= inputMethod.dismiss(this);
            }
            return dismissed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final String lockState;
        if (lock.tryLock()) {
            try {
                lockState = lock.hasWaiters(click) ? "waiting" : "active";
            } finally {
                lock.unlock();
            }
        } else {
            lockState = "locked";
        }
        return (scriptTask != null ? scriptTask.getRelation() + " " : " ") + "" + choices.toString() + " " + lockState
                + (paused.get() ? " paused" : "") + " result=" + toString(result);
    }

    private String toString(int result) {
        if (result == Prompt.UNDEFINED) {
            return "UNDEFINED";
        } else if (result == Prompt.DISMISSED) {
            return "DISMISSED";
        } else {
            return choice(result);
        }
    }

}
