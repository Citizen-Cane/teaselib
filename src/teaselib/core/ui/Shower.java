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
        // TODO SR input depends on script actor locale
        // -> move initialization to TeaseScriptBase
        // TODO pass input methods to prompt
        // TODO input method must be final - no state besides prompt?
        // TODO prompt should hold input methods
        // - static like HostInput
        // - dynamic for speech recognition
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
        // acquireLock();

        try {
            stack.push(prompt);
            prompt.executeScriptTask(script, promptPipeline);
            while (true) {
                if (stack.peek() == prompt) {
                    // TODO SR
                    int resultIndex = promptPipeline.show(prompt);
                    if (resultIndex == Prompt.PAUSED) {
                        prompt.pauseUntilResumed();
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
        } finally {
        }
    }

    private void pauseCurrent() {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) {
        prompt.enterPause();
        promptPipeline.dismiss(prompt);

        if (!prompt.pauseRequested()) {
            throw new IllegalStateException("Stack element " + stack.peek() + "Not paused");
        }

        // TODO Sort out
        // if (!prompt.pausing()) {
        // throw new IllegalStateException("Stack element " + stack.peek() //
        // + "not waiting on lock");
        // }
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
            Prompt prompt = stack.peek();
            prompt.resume();
        }
    }
}
