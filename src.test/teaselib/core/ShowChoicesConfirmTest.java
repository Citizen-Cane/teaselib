package teaselib.core;

import static org.junit.Assert.*;
import static teaselib.ScriptFunction.*;
import static teaselib.core.speechrecognition.TimeoutBehavior.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.Debugger.Response;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesConfirmTest {

    @Test
    public void testDismissScriptFunction() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";
            script.debugger.addResponse(choice, Debugger.Response.Choose);
            // TODO debug response input method fails to apply since the prompt is dismissed already
            // -> affects tests in Mine since the flow of execution is changed
            for (int i = 0; i < 10; i++) {
                assertEquals(choice, script.reply(script.timeoutWithConfirmation(3, InDubioContraReum), choice));
            }
        }
    }

    @Test
    public void testDismissScriptFunctionRealTime() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";
            script.debugger.addResponse(choice, Debugger.Response.Choose);
            script.debugger.resumeTime();
            for (int i = 0; i < 10; i++) {
                assertEquals(choice, script.reply(script.timeoutWithConfirmation(1, InDubioContraReum), choice));
            }
        }
    }

    @Test
    public void testAwaitScriptFunctionConfirmTimeout() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";

            script.debugger.addResponse(new Debugger.ResponseAction(choice, chooseAfterTimeout(script, 2000)));
            assertEquals(TimeoutString, script.reply(script.timeoutWithConfirmation(1, InDubioContraReum), choice));
        }
    }

    @Test
    public void testAwaitScriptFunctionConfirmTimeoutRealTime() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";

            script.debugger.resumeTime();
            script.debugger.addResponse(new Debugger.ResponseAction(choice, chooseAfterTimeout(script, 2000)));
            assertEquals(TimeoutString, script.reply(script.timeoutWithConfirmation(1, InDubioContraReum), choice));
        }
    }

    @Test
    public void testAwaitScriptFunctionAutoConfirmTimeout() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";

            script.debugger.addResponse(new Debugger.ResponseAction(choice, chooseAfterTimeout(script, 2000)));
            assertEquals(TimeoutString, script.reply(script.timeoutWithAutoConfirmation(1, InDubioContraReum), choice));
        }
    }

    @Test
    public void testAwaitScriptFunctionAutoConfirmTimeoutRealTime() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Foobar";

            script.debugger.resumeTime(); // TODO resolve huge timeout value -> 2000 should be enough
            script.debugger.addResponse(new Debugger.ResponseAction(choice, chooseAfterTimeout(script, 5000)));
            assertEquals(TimeoutString, script.reply(script.timeoutWithAutoConfirmation(1, InDubioContraReum), choice));
        }
    }

    private static Callable<Response> chooseAfterTimeout(TestScript script, long timeoutMillis) {
        long startMillis = script.teaseLib.getTime(TimeUnit.MILLISECONDS);
        return () -> {
            long elapsedMillis = script.teaseLib.getTime(TimeUnit.MILLISECONDS);
            long duration = elapsedMillis - startMillis;
            return duration >= timeoutMillis ? Debugger.Response.Choose : Debugger.Response.Ignore;
        };
    }

}
