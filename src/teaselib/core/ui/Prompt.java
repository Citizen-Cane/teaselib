/**
 * 
 */
package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import teaselib.ScriptFunction;
import teaselib.core.Script;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.util.ExceptionUtil;

public class Prompt {
    private static final List<Choice> SCRIPTFUNCTION_TIMEOUT = Collections
            .singletonList(new Choice(ScriptFunction.Timeout, ScriptFunction.Timeout));

    public static class Result {
        public static final Result UNDEFINED = new Result(Integer.MIN_VALUE);
        public static final Result DISMISSED = new Result(-1);

        public final List<Integer> elements;

        public Result(Integer value) {
            this.elements = Collections.singletonList(value);
        }

        public Result(Integer... values) {
            this.elements = Collections.unmodifiableList(Arrays.asList(values));
        }

        public Result(List<Integer> values) {
            this.elements = Collections.unmodifiableList(values);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((elements == null) ? 0 : elements.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Result other = (Result) obj;
            if (elements == null) {
                if (other.elements != null)
                    return false;
            } else if (!elements.equals(other.elements))
                return false;
            return true;
        }

        public boolean equals(Integer value) {
            return elements.size() == 1 && elements.get(0).equals(value);
        }

        public boolean valid(Choices choices) {
            for (Integer value : elements) {
                if (value < 0 || value >= choices.size()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return elements.toString();
        }

    }

    static final String NONE = "None";

    public final Choices choices;
    final List<InputMethod> inputMethods;
    final ScriptFutureTask scriptTask;

    public final ReentrantLock lock;
    public final Condition click;

    private final AtomicBoolean paused = new AtomicBoolean(false);

    private Result result;
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

        this.result = Prompt.Result.UNDEFINED;
    }

    public boolean hasScriptFunction() {
        return scriptTask != null;
    }

    void executeScriptTask() {
        scriptTask.execute();
    }

    void cancelScriptTask() {
        if (scriptTask != null && !scriptTask.isCancelled() && !scriptTask.isDone()) {
            scriptTask.cancel(true);
        }
    }

    // Eliminate result parameter -> it's always called with this.result
    List<Choice> choice(Result result) {
        List<Choice> choice;
        if (result.equals(Prompt.Result.DISMISSED) || result.equals(Prompt.Result.UNDEFINED)) {
            choice = SCRIPTFUNCTION_TIMEOUT;
        } else {
            choice = choices.get(result);
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
        forwardToScriptAndThrow(new IllegalArgumentException("No handler for " + key + " in " + this));
    }

    public synchronized Result result() {
        if (exception != null) {
            if (exception instanceof Exception) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce((Exception) exception));
            } else {
                throw ExceptionUtil.asRuntimeException(exception);
            }
        }
        return result;
    }

    public synchronized void setResultOnce(InputMethod inputMethod, Result result) {
        if (this.result.equals(Prompt.Result.UNDEFINED)) {
            if (!result.valid(choices)) {
                forwardToScriptAndThrow(new IndexOutOfBoundsException(result + "->" + this + ": " + inputMethod));
            } else {
                this.resultInputMethod = inputMethod;
                this.result = result;
            }
        } else {
            forwardAndThrowResultAlreadySet();
        }
    }

    public synchronized void setTimedOut() {
        if (result.equals(Prompt.Result.UNDEFINED)) {
            // TODO Should be TIMED_OUT
            result = Prompt.Result.DISMISSED;
        } else {
            forwardAndThrowResultAlreadySet();
        }
    }

    public void signalResult(InputMethod inputMethod, Result result) {
        setResultOnce(inputMethod, result);

        if (!paused()) {
            click.signalAll();
        } else {
            forwardToScriptAndThrow(new IllegalStateException(inputMethod + " tried to signal paused prompt " + this));
        }
    }

    private void forwardAndThrowResultAlreadySet() {
        String message = "Prompt " + this + " already set to " + resultInputMethod + " -> " + result;
        forwardToScriptAndThrow(new IllegalStateException(message));
    }

    private void forwardToScriptAndThrow(RuntimeException e) {
        setException(e);
        throw e;
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
        this.result = Prompt.Result.UNDEFINED;
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
        return scriptTaskDescription + choices + " " + lockState + isPaused + resultString + inputMethodName;
    }

    private String toString(Prompt.Result result) {
        if (result.equals(Prompt.Result.UNDEFINED)) {
            return "UNDEFINED";
        } else if (result.equals(Prompt.Result.DISMISSED)) {
            return "DISMISSED";
        } else {
            List<Choice> choice = choice(result);
            return choice.stream().map(Choice::Display).collect(Collectors.joining(" "));
        }
    }
}
