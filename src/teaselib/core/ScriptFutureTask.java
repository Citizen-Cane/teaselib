/**
 * 
 */
package teaselib.core;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import teaselib.core.events.Delegate;

public class ScriptFutureTask extends FutureTask<String> {
    public final static class TimeoutClick {
        public boolean clicked = false;
    }

    public final TimeoutClick timeout;

    public ScriptFutureTask(final TeaseScriptBase script,
            final Runnable scriptFunction, final List<String> derivedChoices,
            final TimeoutClick timeout) {
        super(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    scriptFunction.run();
                    // Keep choices available until the last part of
                    // the script function has finished rendering
                    script.completeAll();
                } catch (ScriptInterruptedException e) {
                    // At this point the script function may have added
                    // deferred renderers to the queue.
                    // Avoid executing these renderers with the next
                    // call to renderMessage()
                    script.clearDeferred();
                    return null;
                }
                // Script function finished
                List<Delegate> clickables = script.teaseLib.host
                        .getClickableChoices(derivedChoices);
                if (!clickables.isEmpty()) {
                    Delegate clickable = clickables.get(0);
                    if (clickable != null) {
                        // Signal timeout and click any button
                        timeout.clicked = true;
                        // Click any delegate
                        clickables.get(0).run();
                    } else {
                        // Host implementation is incomplete
                        throw new IllegalStateException(
                                "Host didn't return clickables for choices: "
                                        + derivedChoices.toString());
                    }
                }
                // Now if the script function is interrupted, there may
                // still be deferred renderers set for the next call to
                // renderMessage()
                // These must be cleared, or they will be run with the
                // next renderMessage() call in the main script thread
                return null;
            }
        });
        this.timeout = timeout;
    }
}
