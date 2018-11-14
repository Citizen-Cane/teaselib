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
    private static final Choice SCRIPTFUNCTION_TIMEOUT = new Choice(ScriptFunction.Timeout, ScriptFunction.Timeout);
    public static final int DISMISSED = -1;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    static final String NONE = "None";

    public final Choices choices;
    final List<InputMethod> inputMethods;
    final ScriptFutureTask scriptTask;

    public final ReentrantLock lock;
    public final Condition click;

    private final AtomicBoolean paused = new AtomicBoolean(false);

    private int result;
    private Throwable exception;

    String inputHandlerKey = NONE;
    private InputMethod resultInputMethod;

    public Prompt(Choices choices, List<InputMethod> inputMethods) {
        this(null, choices, inputMethods, null);
    }

    public Prompt(Script script, Choices choices, List<InputMethod> inputMethods, ScriptFunction scriptFunction) {
        this.choices = choices;
        this.inputMethods = inputMethods;
        this.scriptTask = scriptFunction != null ? new ScriptFutureTask(script, scriptFunction, this) : null;

        this.lock = new ReentrantLock();
        this.click = lock.newCondition();

        this.result = Prompt.UNDEFINED;
    }

    void executeScriptTask() {
        scriptTask.execute();
    }

    void cancelScriptTask() {
        if (scriptTask != null && !scriptTask.isCancelled() && !scriptTask.isDone()) {
            scriptTask.cancel(true);
        }
    }

    Choice choice(int resultIndex) {
        Choice choice;
        if (resultIndex == Prompt.DISMISSED || resultIndex == UNDEFINED) {
            choice = SCRIPTFUNCTION_TIMEOUT;
        } else {
            choice = choices.get(resultIndex);
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
        if (exception != null) {
            if (exception instanceof Exception) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce((Exception) exception));
            } else {
                throw ExceptionUtil.asRuntimeException(exception);
            }
        }
        return result;
    }

    public synchronized void setResultOnce(InputMethod inputMethod, int value) {
        if (result == Prompt.UNDEFINED) {
            if (value < 0 || value >= choices.size()) {
                throw new IndexOutOfBoundsException(value + "->" + toString() + ": " + inputMethod.toString());
            } else {
                this.resultInputMethod = inputMethod;
                result = value;
            }
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
                if (inputMethod != this.resultInputMethod) {
                    dismissed &= inputMethod.dismiss(this);
                }
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

    public void setException(Throwable throwable) {
        this.exception = throwable;
        this.result = Prompt.UNDEFINED;
        if (lock.isHeldByCurrentThread()) {
            click.signal();
        } else if (lock.tryLock()) {
            try {
                click.signal();
            } finally {
                lock.unlock();
            }
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
        String inputMethodName = resultInputMethod != null
                ? "(input method =" + resultInputMethod.getClass().getSimpleName() + ")"
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
