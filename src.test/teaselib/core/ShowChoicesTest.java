package teaselib.core;


import org.junit.jupiter.api.Test;
import teaselib.ScriptFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowChoicesTest extends ShowChoicesAbstractTest {


    @Test
    public void testSimpleReply() {
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("Main script start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("Main script end.");
    }

    @Test
    public void testSingleScriptFunction() {
        debugger.addResponse("Stop", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Inside script function.");
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testSingleScriptFunctionWithInnerReply() {
        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function.");
            assertEquals("No", script.reply("Yes", "No"));
            script.say("End of script function.");
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testTwoScriptFunctionsEachWithInnerReply() {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                script.say("End of script function 2");

            }, "Stop script function 2"));

            script.say("End of script function 1.");

        }, "Stop script function 1"));
        script.say("Resuming main script");
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*2", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");
                    assertEquals("No Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                    script.say("End of script function 3");

                }, "Stop script function 3"));

                script.say("End of script function 2");

            }, "Stop script function 2"));

            script.say("End of script function 1.");

        }, "Stop script function 1"));
        script.say("Resuming main script");
    }

}
