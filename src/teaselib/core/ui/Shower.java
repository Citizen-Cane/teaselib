package teaselib.core.ui;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import teaselib.Answer;
import teaselib.ScriptFunction.AnswerOverride;
import teaselib.core.ScriptFutureTask;
import teaselib.core.util.ExceptionUtil;

public class Shower {

    private final PromptQueue promptQueue;

    public Shower() {
        this.promptQueue = new PromptQueue();
    }

    public List<Choice> show(Prompt prompt) throws InterruptedException {
        List<Choice> choice;
        prompt.lock.lockInterruptibly();
        try {
            promptQueue.pauseCurrent();
            choice = showNew(prompt);
            promptQueue.resumePrevious();
            return answer(prompt, choice);
        } finally {
            prompt.lock.unlock();
        }
    }

    private static List<Choice> answer(Prompt prompt, List<Choice> choice) throws InterruptedException {
        var scriptTask = prompt.scriptTask;
        if (scriptTask != null) {
            try {
                var answer = scriptTask.get();
                if (answer != null) {
                    return Collections.singletonList(new Choice(answer));
                } else if (prompt.undefined() || prompt.dismissed()) {
                    return Collections.singletonList(new Choice(Answer.Timeout));
                } else {
                    return choice;
                }
            } catch (CancellationException e) {
                Throwable exception = scriptTask.getException();
                if (exception == null) {
                    return choice;
                } else if (exception instanceof AnswerOverride override) {
                    return Collections.singletonList(new Choice(override.answer));
                } else {
                    throw ExceptionUtil.asRuntimeException(exception);
                }
            } catch (ExecutionException e) {
                Throwable exception = scriptTask.getException();
                if (exception == null) {
                    throw ExceptionUtil.asRuntimeException(e);
                } else {
                    throw ExceptionUtil.asRuntimeException(exception);
                }
            }
        } else {
            return choice;
        }
    }

    private static void throwScriptTaskException(Prompt prompt) throws InterruptedException {
        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null && !scriptTask.isCancelled()) {
            boolean isInterrupted = Thread.interrupted(); // clear to be able to wait
            try {
                scriptTask.awaitCompleted();
            } finally {
                if (isInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private List<Choice> showNew(Prompt prompt) throws InterruptedException {
        try {
            promptQueue.show(prompt);
            return result(prompt);
        } catch (Exception e) {
            promptQueue.resumePreviousPromptAfterException(prompt, e);
            throwScriptTaskException(prompt);
            throw e;
        }
    }

    private List<Choice> result(Prompt prompt) throws InterruptedException {
        while (prompt.undefined()) {
            var eventArgs = prompt.inputMethodEventArgs.getAndSet(InputMethodEventArgs.None);
            if (eventArgs != InputMethodEventArgs.None) {
                promptQueue.invokeHandler(prompt, eventArgs);
                promptQueue.awaitResult(prompt);
            }
        }
        return cancelScriptTaskAndReturnResult(prompt);
    }

    private static List<Choice> cancelScriptTaskAndReturnResult(Prompt prompt) {
        prompt.cancelScriptTask();
        return prompt.choice();
    }

    public void updateUI(InputMethod.UiEvent event) {
        promptQueue.updateUI(event);
    }

}
