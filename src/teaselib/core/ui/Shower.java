package teaselib.core.ui;

import java.util.Stack;

import teaselib.core.Host;
import teaselib.core.TeaseScriptBase;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<Prompt>();

    private final PromptQueue promptPipeline;

    public Shower(Host host) {
        this.host = host;
        this.promptPipeline = new PromptQueue();
    }

    public String show(TeaseScriptBase script, Prompt prompt) {
        pauseCurrent();

        try {
            return showNew(script, prompt);
        } finally {
            resumePrevious();
        }
    }

    private String showNew(TeaseScriptBase script, Prompt prompt) {
        stack.push(prompt);
        prompt.executeScriptTask(script, promptPipeline);
        while (true) {
            if (stack.peek() == prompt) {
                int resultIndex = promptPipeline.show(prompt);
                if (resultIndex == Prompt.PAUSED) {
                    // prompt.pauseUntilResumed();
                    // TODO pause state is set to handle paused prompts - avoid
                    // this!
                    throw new IllegalStateException("PAUSED is not a valid return value for " + prompt);
                } else if (resultIndex == Prompt.DISMISSED) {
                    if (prompt.scriptTask != null) {
                        prompt.scriptTask.join();
                        prompt.forwardErrorsAsRuntimeException();
                    }
                    String choice = prompt.choice(resultIndex);
                    return choice;
                } else {
                    prompt.completeScriptTask();
                    return prompt.choice(resultIndex);
                }
            } else {
                prompt.pauseUntilResumed();
            }
        }
    }

    private void pauseCurrent() {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) {
        prompt.enterPause();
        // TODO must submit pause to restore prompt later on
        // but runs into IllegalMonitorException in HostInputMethod
        promptPipeline.dismiss(prompt, Prompt.PAUSED);
        // promptPipeline.dismiss(prompt);

        if (!prompt.pauseRequested()) {
            throw new IllegalStateException("Stack element " + stack.peek() + "Not paused");
        }
    }

    private void resumePrevious() {
        if (!stack.isEmpty()) {
            Prompt prompt = stack.peek();
            synchronized (prompt) {
                if (promptPipeline.getActive() == prompt) {
                    promptPipeline.dismiss(prompt);
                }
                stack.pop();
            }
        } else {
            throw new IllegalStateException("Prompt stack empty");
        }

        if (!stack.isEmpty()) {
            // Prompt prompt = stack.peek();
            // prompt.resume();
            promptPipeline.resume(stack.peek());
        }
    }
}
