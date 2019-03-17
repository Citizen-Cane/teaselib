package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                resumeAfterException();
                throwScriptTaskException(prompt);
                throw e;
            }

            resumePrevious();
        } finally {
            prompt.lock.unlock();
        }

        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null) {
            try {
                return Collections.singletonList(new Choice(scriptTask.get()));
            } catch (CancellationException e) {
                return choice;
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
        } else {
            return choice;
        }
    }

    private void throwScriptTaskException(Prompt prompt) throws InterruptedException {
        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null) {
            try {
                scriptTask.get();
            } catch (CancellationException ignore) {
                // ignore
            } catch (ExecutionException e1) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e1));
            }
        }
    }

    private void resumeAfterException() {
        try {
            resumePrevious();
        } catch (Exception ignore) {
            logger.error(ignore.getMessage(), ignore);
        }
    }

    private List<Choice> showNew(Prompt prompt) throws InterruptedException {
        stack.push(prompt);

        Prompt.Result result = promptQueue.show(prompt);
        return result(prompt, result);
    }

    private List<Choice> result(Prompt prompt, Prompt.Result result) throws InterruptedException {
        if (result.equals(Prompt.Result.DISMISSED)) {
            // TODO join with else-branch, but the DISMISSED case must be first
            prompt.cancelScriptTask();
            return prompt.choice(result);
        } else if (prompt.inputHandlerKey != Prompt.NONE) {
            try {
                invokeHandler(prompt);
            } finally {
                prompt.inputHandlerKey = Prompt.NONE;
            }
            if (promptQueue.getActive() != prompt) {
                promptQueue.resume(prompt);
            }
            return result(prompt, promptQueue.awaitResult(prompt));
        } else {
            prompt.cancelScriptTask();
            return prompt.choice(result);
        }
    }

    private void invokeHandler(Prompt prompt) {
        if (!prompt.result().equals(Prompt.Result.UNDEFINED)) {
            throw new IllegalStateException("Prompt selected while invoking handler: " + prompt);
        }

        if (!executingAlready(prompt.inputHandlerKey)) {
            prompt.executeInputMethodHandler();
        }

        if (stack.peek() != prompt) {
            throw new IllegalStateException("Not top-most: " + prompt);
        }
    }

    private boolean executingAlready(String inputHandlerKey) {
        Prompt current = stack.peek();
        for (Prompt prompt : stack) {
            if (prompt != current && prompt.inputHandlerKey == inputHandlerKey) {
                return true;
            }
        }
        return false;
    }

    private void pauseCurrent() {
        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();
            if (!prompt.paused()) {
                pause(prompt);
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
            promptQueue.resume(stack.peek());
        }
    }
}
