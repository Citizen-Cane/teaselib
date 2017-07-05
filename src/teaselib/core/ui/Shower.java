package teaselib.core.ui;

import java.util.Stack;

import teaselib.core.Host;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.TeaseScriptBase;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<Prompt>();

    private final PromptQueue promptQueue;

    public Shower(Host host) {
        this.host = host;
        this.promptQueue = new PromptQueue();
    }

    public String show(TeaseScriptBase script, Prompt prompt) {
        try {
            pauseCurrent();
        } catch (InterruptedException e1) {
            throw new ScriptInterruptedException();
        }

        try {
            return showNew(script, prompt);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            try {
                resumePrevious();
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
        }
    }

    private String showNew(TeaseScriptBase script, Prompt prompt) throws InterruptedException {
        stack.push(prompt);
        if (prompt.scriptFunction != null) {
            prompt.executeScriptTask(script, promptQueue.getDismissCallable(prompt));
        }

        while (true) {
            if (stack.peek() == prompt) {
                int resultIndex = promptQueue.show(prompt);
                if (resultIndex == Prompt.DISMISSED) {
                    prompt.dismissScriptTask();
                    prompt.forwardErrorsAsRuntimeException();
                    return prompt.choice(resultIndex);
                } else if (resultIndex == SpeechRecognitionInputMethod.RECOGNITION_REJECTED) {
                    // TODO query if registered instead of hard-coding constants
                    // here
                    executeHandler(script, resultIndex);
                    // TODO replay missing
                    promptQueue.clear(prompt);
                } else {
                    prompt.completeScriptTask();
                    prompt.forwardErrorsAsRuntimeException();
                    return prompt.choice(resultIndex);
                }
            } else {
                throw new IllegalStateException("Explicit prompt pausing is deprecated");
            }
        }
    }

    private static void executeHandler(TeaseScriptBase script, int id) {
        if (id == SpeechRecognitionInputMethod.RECOGNITION_REJECTED) {
            script.actor.speechRecognitionRejectedScript.run();
        } else {
            throw new IllegalArgumentException("No handler for id " + id);
        }
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
            synchronized (prompt) {
                // TODO Possibly not necessary
                if (promptQueue.getActive() == prompt) {
                    promptQueue.dismiss(prompt);
                }
                stack.pop();
                promptQueue.clear(prompt);
            }
        } else {
            throw new IllegalStateException("Prompt stack empty");
        }

        if (!stack.isEmpty()) {
            promptQueue.resume(stack.peek());
        }
    }
}
