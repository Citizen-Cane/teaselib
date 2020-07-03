package teaselib.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

public class ScriptEventInputMethod extends AbstractInputMethod {

    public enum Notification implements InputMethod.Notification {
        ScriptEvent
    }

    public ScriptEventInputMethod(ExecutorService executor) {
        super(executor);
    }

    @Override
    public Setup getSetup(Choices choices) {
        return Unused;
    }

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        Thread.sleep(Long.MAX_VALUE);
        return Prompt.Result.UNDEFINED;
    }

    @Override
    protected void handleDismiss(Prompt prompt) throws InterruptedException {
        // Nothing to do
    }

    /**
     * if there is an active prompt, execute the action in the script thread.
     * 
     * @param action
     */
    public boolean signalEvent(Runnable action) {
        return signalActivePrompt(action, new ScriptEventInputMethodEventArgs(Notification.ScriptEvent));
    }

}
