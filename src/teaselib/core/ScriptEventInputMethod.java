package teaselib.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import teaselib.core.ui.AbstractInputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.Prompt.Result;

public class ScriptEventInputMethod extends AbstractInputMethod {
    private static final String EVENT_HANDLER_KEY = "Event Received";

    AtomicReference<Prompt> active;

    public ScriptEventInputMethod(ExecutorService executor) {
        super(executor);
    }

    @Override
    protected Result handleShow(Prompt prompt) throws InterruptedException, ExecutionException {
        Thread.sleep(Long.MAX_VALUE);
        return Prompt.Result.UNDEFINED;
    }

    @Override
    protected boolean handleDismiss(Prompt prompt) throws InterruptedException {
        return false;
    }

    public void signalEvent(Runnable action) {
        Handler handler = new Handler(EVENT_HANDLER_KEY, action);
        add(handler);
        try {
            signal(handler);
        } finally {
            remove(handler);
        }
    }

}
