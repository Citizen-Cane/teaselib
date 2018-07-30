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
    private static final Choice TIMEOUT = new Choice(ScriptFunction.Timeout, ScriptFunction.Timeout);
    public static final int DISMISSED = -1;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    static final String NONE = "None";

    public final Choices choices;
    public final ScriptFunction scriptFunction;

    final List<InputMethod> inputMethods;
    // TODO script task can be final -> remove script function
    ScriptFutureTask scriptTask;

    public final ReentrantLock lock;
    public final Condition click;

    final AtomicBoolean paused = new AtomicBoolean(false);

    private int result;

    String inputHandlerKey = NONE;
    private InputMethod inputMethod;

    public Prompt(Choices choices, ScriptFunction scriptFunction, List<InputMethod> inputMethods) {
        super();
        this.choices = choices;
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

    void joinScriptTask() throws InterruptedException {
        if (scriptTask != null) {
            scriptTask.join();
        }
    }

    void cancelScriptTask() throws InterruptedException {
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

    Choice choice(int resultIndex) {
        if (scriptTask != null && scriptTask.timedOut()) {
            return TIMEOUT;
        } else {
            Choice choice = scriptTask != null ? scriptTaskResult(scriptTask) : null;
            if (choice == null) {
                if (resultIndex == Prompt.DISMISSED) {
                    choice = TIMEOUT;
                } else if (resultIndex == UNDEFINED) {
                    throw new IllegalStateException("Undefined prompt result for " + this);
                } else {
                    choice = choices.get(resultIndex);
                }
            }
            return choice;
        }
    }

    private static Choice scriptTaskResult(ScriptFutureTask scriptTask) {
        String scriptFunctionResult = scriptTask.getScriptFunctionResult();
        if (scriptFunctionResult != null) {
            return new Choice(scriptFunctionResult, scriptFunctionResult);
        } else {
            return null;
        }
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

    public synchronized void setResultOnce(InputMethod inputMethod, int value) {
        if (result == Prompt.UNDEFINED) {
            this.inputMethod = inputMethod;
            result = value;
        } else {
            throw new IllegalStateException("Prompt " + this + " already set to " + inputMethod + " -> " + value);
        }
    }

    public void signalResult(InputMethod inputMethod, int resultIndex) {
        setResultOnce(inputMethod, resultIndex);

        if (!paused()) {
            click.signalAll();
        } else {
            throw new IllegalStateException(inputMethod + " tried to signal paused prompt " + this);
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
        String scriptTaskDescription = scriptTask != null ? scriptTask.getRelation() + " " : " ";
        String isPaused = paused.get() ? " paused" : "";
        String resultString = " result=" + toString(result);
        String inputMethodName = inputMethod != null ? "(input method =" + inputMethod.getClass().getSimpleName() + ")"
                : "";
        return scriptTaskDescription + choices.toString() + " " + lockState + isPaused + resultString + inputMethodName;
    }

    private String toString(int result) {
        if (result == Prompt.UNDEFINED) {
            return "UNDEFINED";
        } else if (result == Prompt.DISMISSED) {
            return "DISMISSED";
        } else {
            return choice(result).display;
        }
    }

}
