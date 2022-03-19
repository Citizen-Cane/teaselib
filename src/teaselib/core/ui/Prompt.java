/**
 * 
 */
package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import teaselib.Answer;
import teaselib.ScriptFunction;
import teaselib.core.Script;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ui.InputMethod.UiEvent;
import teaselib.core.util.ExceptionUtil;

public class Prompt {
    private static final List<Choice> SCRIPTFUNCTION_TIMEOUT = Collections.singletonList(new Choice(Answer.Timeout));

    public static final Supplier<InputMethod.UiEvent> AlwaysEnabled = () -> new InputMethod.UiEvent(true);
    public static final InputMethod.UiEvent AlwaysDisabled = new InputMethod.UiEvent(false);

    public static class Result {
        public static final Result UNDEFINED = new Result(Integer.MIN_VALUE) {
            @Override
            public String toString() {
                return "UNDEFINED";
            }
        };
        public static final Result DISMISSED = new Result(-1) {
            @Override
            public String toString() {
                return "DISMISSED";
            }
        };

        public enum Accept {
            Distinct,
            Multiple
        }

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

    public final Script script;
    public final Choices choices;

    public final Result.Accept acceptedResult;

    final InputMethods inputMethods;
    public final InputMethods.Initializers inputMethodInitializers;
    private final Supplier<InputMethod.UiEvent> initialUiEventSupplier;
    private InputMethod.UiEvent initialUiEvent = AlwaysDisabled;

    final ScriptFutureTask scriptTask;

    public final ReentrantLock lock;
    public final Condition click;
    private final Condition uiChanged;

    private final AtomicBoolean paused = new AtomicBoolean(false);
    final Map<InputMethod.Notification, Action> inputMethodEventActions = new HashMap<>();
    final AtomicReference<InputMethodEventArgs> inputMethodEventArgs = new AtomicReference<>(InputMethodEventArgs.None);

    private List<InputMethod> realized = Collections.emptyList();
    private Result result;
    private Throwable exception;
    private InputMethod resultInputMethod;

    Prompt() {
        this(null, null);
    }

    public Prompt(Choices choices, InputMethods inputMethods) {
        this(null, choices, inputMethods, null, Result.Accept.Distinct);
    }

    public Prompt(Choices choices, InputMethods inputMethods, Result.Accept mode) {
        this(null, choices, inputMethods, null, mode);
    }

    public Prompt(Script script, Choices choices, InputMethods inputMethods, ScriptFunction scriptFunction,
            Result.Accept mode) {
        this(script, choices, inputMethods, scriptFunction, mode, AlwaysEnabled);
    }

    public Prompt(Script script, Choices choices, InputMethods inputMethods, ScriptFunction scriptFunction,
            Result.Accept mode, Supplier<InputMethod.UiEvent> initialUiEventSupplier) {
        this.script = script;
        this.choices = choices;
        this.inputMethods = inputMethods;
        this.inputMethodInitializers = inputMethods.initializers(choices);
        this.initialUiEventSupplier = initialUiEventSupplier;
        this.initialUiEvent = AlwaysDisabled;

        this.scriptTask = scriptFunction != null ? new ScriptFutureTask(script, scriptFunction, this) : null;
        this.acceptedResult = mode;

        this.lock = new ReentrantLock();
        this.click = lock.newCondition();
        this.uiChanged = lock.newCondition();

        this.result = Prompt.Result.UNDEFINED;
    }

    public boolean hasScriptTask() {
        return scriptTask != null;
    }

    void executeScriptTask() {
        scriptTask.execute();
    }

    void cancelScriptTask() {
        throwIfNotLocked();
        throwIfPaused("Cannot cancel script function");

        if (scriptTask != null && !scriptTask.isCancelled() && !scriptTask.isDone()) {
            scriptTask.cancel(true);
        }
    }

    List<Choice> choice() {
        return choice(choices, result());
    }

    private static List<Choice> choice(Choices choices, Prompt.Result result) {
        if (result.equals(Prompt.Result.DISMISSED) || result.equals(Prompt.Result.UNDEFINED)) {
            return SCRIPTFUNCTION_TIMEOUT;
        } else {
            return choices.get(result);
        }
    }

    void executeInputMethodHandler(InputMethodEventArgs eventArgs) {
        throwIfNotLocked();

        try {
            Action action;
            synchronized (inputMethodEventActions) {
                action = inputMethodEventActions.get(eventArgs.source);
            }
            if (action != null) {
                action.run(eventArgs);
                script.awaitMandatoryCompleted();
            }
        } finally {
            inputMethodEventArgs.set(InputMethodEventArgs.None);
        }
    }

    public void show() throws InterruptedException {
        throwIfNotLocked();
        throwIfPaused();

        initialUiEvent = initialUiEventSupplier.get();
        while (!initialUiEvent.enabled) {
            uiChanged.await();
        }

        realized = inputMethods.selected(choices);
        for (InputMethod inputMethod : realized) {
            inputMethod.show(this);
        }
    }

    boolean isActive() {
        return !realized.isEmpty();
    }

    public boolean dismissed() {
        return result().equals(Result.DISMISSED);
    }

    public boolean undefined() {
        return result().equals(Result.UNDEFINED);
    }

    public Result result() {
        throwIfNotLocked();
        return unsynchronizedResult();
    }

    public Result unsynchronizedResult() {
        if (exception != null) {
            if (exception instanceof Exception) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce((Exception) exception));
            } else {
                throw ExceptionUtil.asRuntimeException(exception);
            }
        }
        return result;
    }

    public void setResultOnce(InputMethod inputMethod, Result result) {
        throwIfNotLocked();
        throwIfPaused(inputMethod + " tried to set result");

        if (result.equals(Prompt.Result.DISMISSED)) {
            forwardToScriptAndThrow(
                    new IllegalArgumentException(result + " is not a valid choice: " + this + ": " + inputMethod));
        } else if (this.result.equals(Prompt.Result.UNDEFINED)) {
            if (!result.valid(choices)) {
                forwardToScriptAndThrow(new IndexOutOfBoundsException(
                        result + " is not a valid choice for " + this + ": " + inputMethod));
            } else {
                this.resultInputMethod = inputMethod;
                this.result = result;
            }
        } else {
            forwardAndThrowResultAlreadySet();
        }
    }

    public void setTimedOut() {
        throwIfNotLocked();
        throwIfPaused("Trying to timeout");

        if (result.equals(Prompt.Result.UNDEFINED)) {
            result = Prompt.Result.DISMISSED;
        } else {
            forwardAndThrowResultAlreadySet();
        }
    }

    public void signal(InputMethod inputMethod, Result r) {
        throwIfNotLocked();
        throwIfPaused(inputMethod + " tried to signal result");

        setResultOnce(inputMethod, r);
        click.signalAll();
    }

    private void forwardAndThrowResultAlreadySet() {
        String message = "Prompt " + this + " already set to "
                + (resultInputMethod != null ? resultInputMethod : "timeout") + " -> " + result + "="
                + choices.get(result.elements.get(0));
        forwardToScriptAndThrow(new IllegalStateException(message));
    }

    private void forwardToScriptAndThrow(RuntimeException e) {
        setException(e);
        throw e;
    }

    public void pause() {
        throwIfNotLocked();
        paused.set(true);
    }

    public boolean paused() {
        throwIfNotLocked();
        return paused.get();
    }

    public void resume() {
        throwIfNotLocked();
        paused.set(false);
    }

    public void updateUI(UiEvent event) {
        initialUiEvent = event;
        uiChanged.signalAll();
        for (InputMethod inputMethod : realized) {
            inputMethod.updateUI(event);
        }
    }

    public void dismiss() {
        initialUiEvent = Prompt.AlwaysDisabled;
        throwIfNotLocked();
        dismissInputMethods().ifPresent(this::throwInputMethodException);
    }

    private Optional<RuntimeException> dismissInputMethods() {
        Optional<RuntimeException> catched = Optional.empty();
        try {
            for (InputMethod inputMethod : realized) {
                try {
                    inputMethod.dismiss(this);
                } catch (Exception e) {
                    catched = Optional.of(new RuntimeException(e));
                }
            }
        } finally {
            realized = Collections.emptyList();
        }
        return catched;
    }

    private void throwInputMethodException(RuntimeException e) {
        throw e;
    }

    public void setException(Throwable throwable) {
        this.exception = throwable;
        this.result = Prompt.Result.UNDEFINED;
        if (lock.isHeldByCurrentThread()) {
            click.signalAll();
        } else if (lock.tryLock()) {
            try {
                click.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public interface Action {
        boolean canRun(InputMethodEventArgs eventArgs);

        void run(InputMethodEventArgs eventArgs);
    }

    public class EventSource {
        final InputMethod.Notification handler;

        public EventSource(InputMethod.Notification eventType) {
            this.handler = eventType;
        }

        public void then(Action action) {
            synchronized (Prompt.this.inputMethodEventActions) {
                Prompt.this.inputMethodEventActions.put(handler, action);
            }
        }
    }

    public EventSource when(InputMethod.Notification eventType) {
        return new EventSource(eventType);
    }

    public void remove(InputMethod.Notification eventType) {
        synchronized (inputMethodEventActions) {
            inputMethodEventActions.remove(eventType);
        }
    }

    public void signal(InputMethodEventArgs eventArgs) {
        throwIfNotLocked();
        throwIfPaused(eventArgs + " tried to signal handler invocation");

        Action action;
        synchronized (inputMethodEventActions) {
            action = inputMethodEventActions.get(eventArgs.source);
        }

        if (action != null && action.canRun(eventArgs)) {
            inputMethodEventArgs.set(eventArgs);
            click.signalAll();
        }
    }

    private void throwIfNotLocked() {
        if (!lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Prompt must be locked by current script function thread: " + this);
        }
    }

    private void throwIfPaused() {
        throwIfPaused("Trying to invoke active-only operation");
    }

    private void throwIfPaused(String reason) {
        if (paused()) {
            throw new IllegalStateException(reason + " on paused prompt: " + this);
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
        var resultString = " result=" + toString(result, choice(choices, result));
        String inputMethodName = resultInputMethod != null
                ? "(input method =" + resultInputMethod.getClass().getSimpleName() + ")"
                : "";
        return scriptTaskDescription + choices + " " + lockState + isPaused + resultString + inputMethodName;
    }

    private static String toString(Prompt.Result result, List<Choice> choice) {
        if (result.equals(Prompt.Result.UNDEFINED)) {
            return "UNDEFINED";
        } else if (result.equals(Prompt.Result.DISMISSED)) {
            return "DISMISSED";
        } else {
            return choice.stream().map(Choice::getDisplay).collect(Collectors.joining(" "));
        }
    }

}
