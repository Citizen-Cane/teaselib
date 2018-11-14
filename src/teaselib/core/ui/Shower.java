package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Host;
import teaselib.core.Script;
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

    public Choice show(Script script, Prompt prompt) throws InterruptedException {
        Choice choice;
        prompt.lock.lockInterruptibly();
        try {
            pauseCurrent();

            try {
                choice = showNew(prompt);
            } catch (Exception e) {
                resumeAfterException();
                throw e;
            }

            resumePrevious();
        } finally {
            prompt.lock.unlock();
        }

        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null && !scriptTask.isCancelled() && scriptTask.isDone()) {
            try {
                return new Choice(scriptTask.get());
            } catch (ExecutionException e) {
                throw ExceptionUtil.asRuntimeException(ExceptionUtil.reduce(e));
            }
        } else {
            return choice;
        }
    }

    private void resumeAfterException() {
        try {
            resumePrevious();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Choice showNew(Prompt prompt) throws InterruptedException {
        stack.push(prompt);

        int resultIndex = promptQueue.show(prompt);
        return result(prompt, resultIndex);
    }

    private Choice result(Prompt prompt, int resultIndex) throws InterruptedException {
        if (resultIndex == Prompt.DISMISSED) {
            return prompt.choice(resultIndex);
        } else if (prompt.inputHandlerKey != Prompt.NONE) {
            try {
                invokeHandler(prompt);
            } finally {
                prompt.inputHandlerKey = Prompt.NONE;
            }
            if (promptQueue.getActive() != prompt) {
                promptQueue.resume(prompt);
            }
            return result(prompt, promptQueue.showExisting(prompt));
        } else {
            prompt.cancelScriptTask();
            return prompt.choice(resultIndex);
        }
    }

    private void invokeHandler(Prompt prompt) {
        if (prompt.result() != Prompt.UNDEFINED) {
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
