package teaselib.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import teaselib.ScriptFunction;

public class ShowChoicesErrorHandlingTest extends ShowChoicesAbstractTest {

   public enum Throw {
        RIGHT_AT_START,
        AFTER_FIRST_QUESTION
    }

    @ParameterizedTest
    @EnumSource(Throw.class)
    public void testSingleScriptFunctionErrorHandling(Throw when) {
        debugger.addResponse("Stop", Debugger.Response.Ignore);

        script.say("In main script.");
        assertThrows(TestException.class, () -> script.reply(() -> {
            if (when == Throw.RIGHT_AT_START) throwTestException();
            script.say("Inside script function.");
            script.awaitAllCompleted();
            if (when == Throw.AFTER_FIRST_QUESTION) throwTestException();
        }, "Stop"));
    }

    @ParameterizedTest
    @EnumSource(Throw.class)
    public void testSingleScriptFunctionWithInnerReplyErrorHandling(Throw when) {
        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(TestException.class, () -> script.reply(() -> {
            script.say("Start of script function.");
            if (when == Throw.RIGHT_AT_START) throwTestException();
            assertEquals("No", script.reply("Yes", "No"));
            if (when == Throw.AFTER_FIRST_QUESTION) throwTestException();
            script.say("End of script function.");
        }, "Stop"));
    }

    @ParameterizedTest
    @EnumSource(Throw.class)
    public void testTwoScriptFunctionsEachWithInnerReplyErrorHandling(Throw when) {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(TestException.class, () -> script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));
            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                if (when == Throw.RIGHT_AT_START) throwTestException();
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                script.say("End of script function 2");
                script.awaitAllCompleted();
                if (when == Throw.AFTER_FIRST_QUESTION) throwTestException();
            }, "Stop script function 2"));
            failedToForwardException();
        }, "Stop script function 1"));
    }

    @ParameterizedTest
    @EnumSource(Throw.class)
    public void testThreeScriptFunctionsEachWithInnerReplyErrorHandling(Throw when) {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
        debugger.addResponse("No*1", Debugger.Response.Choose);
        debugger.addResponse("Wow*2", Debugger.Response.Choose);
        debugger.addResponse("Oh*3", Debugger.Response.Choose);

        script.say("In main script.");
        assertThrows(TestException.class, () -> script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");
                    if (when == Throw.RIGHT_AT_START) throwTestException();
                    assertEquals("Oh Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                    if (when == Throw.AFTER_FIRST_QUESTION) throwTestException();
                    script.say("End of script function 3");
                }, "Stop script function 3"));
                failedToForwardException();
            }, "Stop script function 2"));
            failedToForwardException();
        }, "Stop script function 1"));
        script.say("Resuming main script");
    }

    private static void failedToForwardException() {
        Assertions.fail("Throwing any exception has to end script");
    }

    private static void throwTestException() {
        throw new TestException("iI script function");
    }
}
