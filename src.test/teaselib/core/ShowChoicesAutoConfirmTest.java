package teaselib.core;

import org.junit.jupiter.api.Test;
import teaselib.ScriptFunction;
import teaselib.core.speechrecognition.TimeoutBehavior;
import teaselib.test.TestScript;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Citizen-Cane
 */
public class ShowChoicesAutoConfirmTest {
    @Test
    public void testDismissScriptFunction() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Dismiss";
            script.debugger.addResponse(choice, Debugger.Response.Choose);

            script.say("Foobar");
            assertEquals(choice,
                    script.reply(script.timeoutWithAutoConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
        }
    }

    @Test
    public void testAwaitScriptFunctionTimeout() throws IOException {
        try (TestScript script = new TestScript()) {
            String choice = "Dismiss";

            script.debugger.addResponse(choice, Debugger.Response.Ignore);

            script.say("Foobar");
            assertEquals(ScriptFunction.TimeoutString,
                    script.reply(script.timeoutWithAutoConfirmation(1, TimeoutBehavior.InDubioContraReum), choice));
        }
    }
}
