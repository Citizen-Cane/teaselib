package teaselib.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.TeaseScript;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptFunctionReturnValueTest {
    private static final String TEST = "test";

    @Test
    public void testThatScriptFunctionWithoutDelayReturnsCorrectValue() {
        TestScript script = TestScript.getOne();
        String wrong = "wrong";

        assertEquals(TEST, script.reply(this::returnImmediately, wrong));
    }

    @Test
    public void testThatScriptFunctionWithDelayReturnsCorrectValue() {
        TestScript script = TestScript.getOne();
        String choice = "wrong";
        script.debugger.addResponse(choice, Debugger.Response.Ignore);

        assertEquals(TEST, script.reply(returnAfterDelay(script), choice));
    }

    private String returnImmediately() {
        return TEST;
    }

    private String returnAfterDelay(TeaseScript script) {
        script.say("something");
        return TEST;
    }
}
