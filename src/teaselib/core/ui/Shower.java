package teaselib.core.ui;

import java.util.Stack;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<>();

    private final PromptQueue promptQueue;

    public Shower(Host host) {
        this.host = host;
        this.promptQueue = new PromptQueue();
    }

    public String show(TeaseScriptBase script, Prompt prompt) {
        try {
            pauseCurrent();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }

        try {
            return showNew(script, prompt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        } finally {
            try {
                resumePrevious();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
        }
    }

    private String showNew(TeaseScriptBase script, Prompt prompt) throws InterruptedException {
        stack.push(prompt);

        while (true) {
            if (stack.peek() == prompt) {
                int resultIndex = promptQueue.show(script, prompt);
                if (resultIndex == Prompt.DISMISSED) {
                    prompt.joinScriptTask();
                    prompt.forwardErrorsAsRuntimeException();
                    return prompt.choice(resultIndex);
                } else if (prompt.inputHandlerKey != Prompt.NONE) {
                    try {
                        invokeHandler(prompt);
                    } finally {
                        prompt.inputHandlerKey = Prompt.NONE;
                        prompt.resume();
                    }
                } else {
                    prompt.cancelScriptTask();
                    prompt.forwardErrorsAsRuntimeException();
                    return prompt.choice(resultIndex);
                }
            } else {
                throw new IllegalStateException("Explicit prompt pausing is deprecated");
            }
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

    private void pauseCurrent() throws InterruptedException {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) throws InterruptedException {
        promptQueue.pause(prompt);
    }

    private void resumePrevious() throws InterruptedException {
        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();

            // TODO Possibly not necessary
            if (promptQueue.getActive() == prompt) {
                promptQueue.dismiss(prompt);
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
