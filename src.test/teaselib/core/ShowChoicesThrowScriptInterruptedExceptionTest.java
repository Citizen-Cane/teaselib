package teaselib.core;

import org.junit.jupiter.api.Test;

import teaselib.ScriptFunction;

import static org.junit.jupiter.api.Assertions.*;

public class ShowChoicesThrowScriptInterruptedExceptionTest extends ShowChoicesAbstractTest {

    @Test
    public void testSingleScriptFunction() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);

        script.say("In main script.");
        assertThrows(ScriptInterruptedException.class, () -> script.reply(() -> {
            script.say("Inside script function.");
            throwScriptInterruptedException();
        }, "Ignore"));
    }

    @Test
    public void testSingleScriptFunctionWithInnerReply() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(ScriptInterruptedException.class, () -> script.reply(() -> {
            script.say("Start of script function.");
            assertEquals("No", script.reply("Yes", "No"));
            throwScriptInterruptedException();
        }, "Ignore"));
    }

    @Test
    public void testTwoScriptFunctionsEachWithInnerReply() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(ScriptInterruptedException.class, () -> script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                throwScriptInterruptedException();
            }, "Ignore script function 2"));
            failedToForwardScriptInterruptedException();
        }, "Ignore script function 1"));
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*1", Debugger.Response.Choose);
        debugger.addResponse("Wow*2", Debugger.Response.Choose);
        debugger.addResponse("Oh*3", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(ScriptInterruptedException.class, () -> script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");
                    assertEquals("Oh Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                    throwScriptInterruptedException();
                }, "Ignore script function 3"));
                failedToForwardScriptInterruptedException();
            }, "Ignore script function 2"));
            failedToForwardScriptInterruptedException();
        }, "Ignore script function 1"));
    }

    private static void failedToForwardScriptInterruptedException() {
        fail("Throwing ScriptInterruptedException has to end script");
    }

}
