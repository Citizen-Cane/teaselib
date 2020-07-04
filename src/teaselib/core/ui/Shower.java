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
import teaselib.core.Host;
import teaselib.core.ScriptFutureTask;
import teaselib.core.util.ExceptionUtil;

public class Shower {
    private static final Logger logger = LoggerFactory.getLogger(Shower.class);

    static final int PAUSED = -1;

    final Host host;
    final Deque<Prompt> stack = new ArrayDeque<>();

    private final PromptQueue promptQueue;

    public Shower(Host host) {
        this.host = host;
        this.promptQueue = new PromptQueue();
    }

    public List<Choice> show(Prompt prompt) throws InterruptedException {
        List<Choice> choice;
        prompt.lock.lockInterruptibly();
        try {
            pauseCurrent();

            try {
                choice = showNew(prompt);
            } catch (Exception e) {
                resumePreviousAfterException();
                throwScriptTaskException(prompt);
                throw e;
            }

            resumePrevious();

            ScriptFutureTask scriptTask = prompt.scriptTask;
            if (scriptTask != null) {
                try {
                    Answer answer = scriptTask.get();
                    if (answer != null) {
                        return Collections.singletonList(new Choice(answer));
                    } else if (prompt.result().equals(Prompt.Result.UNDEFINED)
                            || prompt.result().equals(Prompt.Result.DISMISSED)) {
                        return Collections.singletonList(new Choice(Answer.Timeout));
                    } else {
                        return choice;
                    }
                } catch (CancellationException e) {
                    Throwable exception = scriptTask.getException();
                    if (exception == null) {
                        return choice;
                    } else if (exception instanceof AnswerOverride) {
                        AnswerOverride override = (AnswerOverride) exception;
                        return Collections.singletonList(new Choice(override.answer));
                    } else {
                        throw ExceptionUtil.asRuntimeException(exception);
                    }
                } catch (ExecutionException e) {
                    throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
                }
            } else {
                return choice;
            }
        } finally {
            prompt.lock.unlock();
        }
    }

    private static void throwScriptTaskException(Prompt prompt) throws InterruptedException {
        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null && !scriptTask.isCancelled()) {
            try {
                scriptTask.get();
            } catch (CancellationException ignore) {
                // ignore
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
        }
    }

    private void resumePreviousAfterException() {
        try {
            resumePrevious();
        } catch (Exception ignore) {
            logger.warn(ignore.getMessage(), "");
        }
    }

    private List<Choice> showNew(Prompt prompt) throws InterruptedException {
        stack.push(prompt);

        promptQueue.show(prompt);
        return result(prompt);
    }

    private List<Choice> result(Prompt prompt) throws InterruptedException {
        Prompt.Result result = prompt.result();
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

    private void pauseCurrent() {
        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();
            prompt.lock.lock();
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
            Prompt prompt = stack.peek();

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
}
