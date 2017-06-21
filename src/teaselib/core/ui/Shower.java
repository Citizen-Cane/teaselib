package teaselib.core.ui;

import java.util.Stack;

import teaselib.ScriptFunction;
import teaselib.core.Host;
import teaselib.core.ScriptFutureTask;
import teaselib.core.ScriptInterruptedException;

public class Shower {
    static final int PAUSED = -1;

    final Host host;
    final Stack<Prompt> stack = new Stack<Prompt>();

    public Shower(Host host) {
        this.host = host;
    }

    public String show(Prompt prompt) {
        dismissPrevious();

        try {
            return showNew(prompt);
        } finally {
            restorePrevious();
        }
    }

    private String showNew(Prompt prompt) {
        stack.push(prompt);

        prompt.executeScriptTask();

        while (true) {
            if (stack.peek() == prompt) {
                // TODO SR
                int resultIndex = host.reply(prompt.choices);
                if (prompt.pauseRequested()) {
                    prompt.pauseUntilResumed();
                    if (prompt.scriptTask != null) {
                        if (prompt.scriptTask.isDone()
                                || prompt.scriptTask.isCancelled()) {
                            prompt.scriptTask.join();
                            prompt.scriptTask.forwardErrorsAsRuntimeException();
                            return prompt.scriptTask.getScriptFunctionResult();
                        }
                    }
                } else {
                    prompt.stopScriptTask();

                    host.dismissChoices(prompt.choices);

                    return getChoice(prompt, resultIndex);
                }
            } else {
                prompt.pauseUntilResumed();
            }
        }

    }

    private static String getChoice(Prompt prompt, int resultIndex) {
        ScriptFutureTask scriptTask = prompt.scriptTask;
        if (scriptTask != null) {
            scriptTask.forwardErrorsAsRuntimeException();
        }

        String choice = retrieveResult(prompt, resultIndex, scriptTask);
        return choice;
    }

    private static String retrieveResult(Prompt prompt, int resultIndex,
            ScriptFutureTask scriptTask) {
        String choice = scriptTask != null
                ? scriptTask.getScriptFunctionResult() : null;
        if (choice == null) {
            // TODO SR
            if (scriptTask != null && scriptTask.timedOut()) {
                // Timeout
                choice = ScriptFunction.Timeout;
            } else {
                choice = prompt.choices.get(resultIndex);
            }
        }
        return choice;
    }

    private void dismissPrevious() {
        if (!stack.empty()) {
            pause(stack.peek());
        }
    }

    private void pause(Prompt prompt) {
        prompt.enterPause();
        while (!prompt.pausing()) {
            synchronized (prompt.lock) {
                host.dismissChoices(prompt.choices);
                try {
                    prompt.lock.wait(100);
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                }
            }
        }

        if (!prompt.pauseRequested()) {
            throw new IllegalStateException(
                    "Stack element " + stack.peek() + " not paused");
        }

        if (!prompt.pausing()) {
            throw new IllegalStateException(
                    "Stack element " + stack.peek() + " not waiting on lock");
        }
    }

    private void restorePrevious() {
        stack.pop();

        if (!stack.isEmpty()) {
            stack.peek().resume();
        }
    }
}
