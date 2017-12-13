package teaselib.core.ui;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Host;
import teaselib.core.Script;

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

    public String show(Script script, Prompt prompt) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            pauseCurrent();

            String choice;
            try {
                choice = showNew(script, prompt);
            } catch (Exception e) {
                resumeAfterException();
                throw e;
            }

            resumePrevious();
            return choice;
        } finally {
            prompt.lock.unlock();
        }
    }

    private void resumeAfterException() throws InterruptedException {
        try {
            resumePrevious();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String showNew(Script script, Prompt prompt) throws InterruptedException {
        stack.push(prompt);

        int resultIndex = promptQueue.show(script, prompt);
        return result(prompt, resultIndex);
    }

    private String result(Prompt prompt, int resultIndex) throws InterruptedException {
        if (resultIndex == Prompt.DISMISSED) {
            prompt.joinScriptTask();
            prompt.forwardErrorsAsRuntimeException();
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
            prompt.forwardErrorsAsRuntimeException();
        }

        return prompt.choice(resultIndex);
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
