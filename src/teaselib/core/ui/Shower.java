package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Answer;
import teaselib.ScriptFunction.AnswerOverride;
import teaselib.core.ScriptFutureTask;
import teaselib.core.util.ExceptionUtil;

public class Shower {
    private static final Logger logger = LoggerFactory.getLogger(Shower.class);

    static final int PAUSED = -1;

    final Deque<Prompt> stack = new ArrayDeque<>();

    private final PromptQueue promptQueue;

    public Shower() {
        this.promptQueue = new PromptQueue();
    }

    public List<Choice> show(Prompt prompt) throws InterruptedException {
        List<Choice> choice;
        prompt.lock.lockInterruptibly();
        try {
            pauseCurrent();
            choice = showNew(prompt);
            resumePrevious();

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
                } else if (prompt.result().equals(Prompt.Result.UNDEFINED)
                        || prompt.result().equals(Prompt.Result.DISMISSED)) {
                    return Collections.singletonList(new Choice(Answer.Timeout));
                } else {
                    return choice;
                }
            } catch (CancellationException | ExecutionException e) {
                Throwable exception = scriptTask.getException();
                if (exception == null) {
                    return choice;
                } else if (exception instanceof AnswerOverride) {
                    AnswerOverride override = (AnswerOverride) exception;
                    return Collections.singletonList(new Choice(override.answer));
                } else {
                    throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
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

    private void resumePreviousPromptAfterException(Prompt prompt, Exception e) {
        try {
            if (stack.isEmpty()) {
                throw new IllegalStateException("Unexpected empty stack: " + prompt, e);
            }

            if (prompt.hasScriptTask()) {
                prompt.scriptTask.cancel(true);
                prompt.scriptTask.awaitCompleted();
            }

            if (!stack.isEmpty() && stack.peek() != prompt) {
                throw new IllegalStateException("Nested prompts not dismissed: " + prompt, e);
            }

            // active prompt UI must be unrealized explicitly
            if (prompt == promptQueue.getActive()) {
                if (prompt.result().equals(Prompt.Result.UNDEFINED)) {
                    promptQueue.dismiss(prompt);
                }
            }
            stack.remove(prompt);
            if (!stack.isEmpty()) {
                promptQueue.setActive(stack.peek());
            }

            if (prompt.isActive()) {
                throw new IllegalStateException("Input methods not dismissed: " + prompt);
            }

        } catch (RuntimeException ignore) {
            if (e instanceof InterruptedException) {
                logger.warn(ignore.getMessage(), ignore);
            } else {
                throw ExceptionUtil.asRuntimeException(e);
            }
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            logger.warn(e1.getMessage(), "");
        }
    }

    private List<Choice> showNew(Prompt prompt) throws InterruptedException {
        try {
            stack.push(prompt);
            promptQueue.show(prompt);
            return result(prompt);
        } catch (Exception e) {
            resumePreviousPromptAfterException(prompt, e);
            throwScriptTaskException(prompt);
            throw e;
        }
    }

    private List<Choice> result(Prompt prompt) throws InterruptedException {
        var result = prompt.result();
        if (result.equals(Prompt.Result.DISMISSED)) {
            return cancelScriptTaskAndReturnResult(prompt);
        } else {
            InputMethodEventArgs eventArgs = prompt.inputMethodEventArgs.getAndSet(null);
            if (eventArgs != null) {
                invokeHandler(prompt, eventArgs);
                promptQueue.awaitResult(prompt);
                return result(prompt);
            } else {
                return cancelScriptTaskAndReturnResult(prompt);
            }
        }
    }

    private static List<Choice> cancelScriptTaskAndReturnResult(Prompt prompt) {
        prompt.cancelScriptTask();
        return prompt.choice();
    }

    private void invokeHandler(Prompt prompt, InputMethodEventArgs eventArgs) throws InterruptedException {
        if (promptQueue.getActive() != null) {
            throw new IllegalStateException("No active prompt expected");
        }
        prompt.pause();

        if (!prompt.result().equals(Prompt.Result.UNDEFINED)) {
            throw new IllegalStateException("Prompt result already set when invoking handler: " + prompt);
        }

        if (!executingAlready(eventArgs.source)) {
            prompt.executeInputMethodHandler(eventArgs);
        }

        if (stack.peek() != prompt) {
            throw new IllegalStateException("Not top-most: " + prompt);
        }

        if (prompt.paused()) {
            promptQueue.resume(prompt);
        }
    }

    private boolean executingAlready(InputMethod.Notification eventType) {
        Prompt current = stack.peek();
        for (Prompt prompt : stack) {
            // TODO non-working - blocks since while executing a handler on the prompt, the prompt is locked
            // -> attempt to execute another handler on the same prompt results in a deadlock
            // TODO NPE since prompt.inputMethodEventArgs is temporary and set back to null immediately
            if (prompt != current && prompt.inputMethodEventArgs.get().source == eventType) {
                return true;
            }
        }
        return false;
    }

    private void pauseCurrent() throws InterruptedException {
        if (!stack.isEmpty()) {
            var prompt = stack.peek();
            prompt.lock.lockInterruptibly();
            try {
                if (!prompt.paused()) {
                    pause(prompt);
                }
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    private void pause(Prompt prompt) {
        promptQueue.pause(prompt);
    }

    private void resumePrevious() throws InterruptedException {
        if (!stack.isEmpty()) {
            var prompt = stack.peek();

            if (promptQueue.getActive() == prompt) {
                throw new IllegalStateException("Prompt not dismissed: " + prompt);
            }
            stack.pop();
        } else {
            throw new IllegalStateException("Prompt stack empty");
        }

        if (!stack.isEmpty()) {
            Prompt previous = stack.peek();
            previous.lock.lock();
            try {
                promptQueue.resume(previous);
            } finally {
                previous.lock.unlock();
            }
        }
    }

    public void updateUI(InputMethod.UiEvent event) throws InterruptedException {
        if (!stack.isEmpty()) {
            var prompt = stack.peek();
            prompt.lock.lockInterruptibly();
            try {
                prompt.updateUI(event);
            } finally {
                prompt.lock.unlock();
            }
        }
    }

}
