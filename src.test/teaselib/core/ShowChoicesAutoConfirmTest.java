package teaselib.core;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.core.speechrecognition.SpeechRecognition.TimeoutBehavior;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesAutoConfirmTest {
    @Test
    public void testDismissScriptFunction() {
        TestScript script = TestScript.getOne();
        String choice = "Dismiss";
        script.debugger.addResponse(choice, Debugger.Response.Choose);

        script.say("Foobar");
        assertEquals(choice,
                script.reply(script.timeoutWithAutoConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
    }

    @Test
    public void testAwaitScriptFunctionTimeout() {
        TestScript script = TestScript.getOne();
        String choice = "Dismiss";

        script.debugger.addResponse(choice, Debugger.Response.Ignore);

        script.say("Foobar");
        assertEquals(ScriptFunction.Timeout,
                script.reply(script.timeoutWithAutoConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
    }
}
