package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.core.Debugger.Response;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesConfirmTest {
    @Test
    public void testDismissScriptFunction() {
        TestScript script = TestScript.getOne();
        String choice = "Dismiss";
        script.debugger.addResponse(choice, Debugger.Response.Choose);

        script.say("Foobar");
        assertEquals(choice,
                script.reply(script.timeoutWithConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
    }

    @Test
    public void testAwaitScriptFunctionTimeout() {
        TestScript script = TestScript.getOne();
        String choice = "Dismiss";

        long start = script.teaseLib.getTime(TimeUnit.MILLISECONDS);
        script.debugger.addResponse(new Debugger.ResponseAction(choice, chooseAfterTimeout(script, start, 1000)));

        script.say("Foobar");
        assertEquals(ScriptFunction.TimeoutString,
                script.reply(script.timeoutWithConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
    }

    private static Callable<Response> chooseAfterTimeout(TestScript script, long startMillis,
            @SuppressWarnings("unused") long timeoutMillis) {
        return () -> {
            long elapsedMillis = script.teaseLib.getTime(TimeUnit.MILLISECONDS);
            long duration = elapsedMillis - startMillis;
            // TODO delay simulation is not correct, because it adds up the delays of all threads
            // -> add up for each thread separately to allow comparing to timeoutMillis directly
            long timeoutMillisWorkaround = Long.MAX_VALUE - elapsedMillis;
            return duration == timeoutMillisWorkaround ? Debugger.Response.Choose : Debugger.Response.Ignore;
        };
    }
}
