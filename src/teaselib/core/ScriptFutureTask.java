/**
 * 
 */
package teaselib.core;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import teaselib.ScriptFunction;
import teaselib.TeaseLib;
import teaselib.core.events.Delegate;
import teaselib.core.util.NamedExecutorService;

public class ScriptFutureTask extends FutureTask<String> {
    public static class TimeoutClick {
        public boolean clicked = false;
    }

    private final ScriptFunction scriptFunction;
    private final TimeoutClick timeout;

    private final static ExecutorService Executor = NamedExecutorService
            .newFixedThreadPool(Integer.MAX_VALUE,
                    ShowChoices.class.getName() + " Script Function", 1,
                    TimeUnit.HOURS);

    public ScriptFutureTask(final TeaseScriptBase script,
            final ScriptFunction scriptFunction,
            final List<String> derivedChoices, final TimeoutClick timeout) {
        super(new Callable<String>() {
            @Override
            public String call() throws Exception {
                synchronized (scriptFunction) {
                    try {
                        scriptFunction.run();
                        // Keep choices available until the last part of
                        // the script function has finished rendering
                        if (Thread.interrupted()) {
                            throw new ScriptInterruptedException();
                        }
                        script.completeAll();
                        // Click a button to continue the main thread
                        clickToFinishFunction(script, derivedChoices, timeout);
                        return null;
                    } catch (ScriptInterruptedException e) {
                        // TODO Remove completely because this should be obsolete:
                        // renderers are cancelled in TeaseScriptBase.showChoices
                        // but let's better test it for a while...
                        // script.endAll();
                        throw e;
                    }
                }
            }

            private void clickToFinishFunction(final TeaseScriptBase script,
                    final List<String> derivedChoices,
                    final TimeoutClick timeout) {
                // Script function finished
                List<Delegate> clickables = script.teaseLib.host
                        .getClickableChoices(derivedChoices);
                if (!clickables.isEmpty()) {
                    Delegate clickable = clickables.get(0);
                    if (clickable != null) {
                        // Signal timeout and click any button
                        timeout.clicked = true;
                        TeaseLib.instance().log
                                .info("Script function finished click");
                        // Click any delegate
                        clickables.get(0).run();
                    } else {
                        // Host implementation is incomplete
                        throw new IllegalStateException(
                                "Host didn't return clickables for choices: "
                                        + derivedChoices.toString());
                    }
                }
            }
        });
        this.scriptFunction = scriptFunction;
        this.timeout = timeout;
    }

    public void execute() {
        Executor.execute(this);
    }

    public void join() {
        synchronized (scriptFunction) {
            // Intentionally left blank,
            // we just have to be able to enter the synchronized block
        }
    }

    public boolean timedOut() {
        return timeout.clicked;
    }

    public ScriptFunction.Relation getRelation() {
        return scriptFunction.relation;
    }

    public String getScriptFunctionResult() {
        return scriptFunction.result;
    }
}
