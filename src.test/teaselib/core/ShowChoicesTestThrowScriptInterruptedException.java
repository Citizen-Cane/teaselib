package teaselib.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import teaselib.ScriptFunction;
import teaselib.test.IntegrationTests;

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesTestThrowScriptInterruptedException extends ShowChoicesAbstractTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[ITERATIONS][0]);
    }

    @Before
    public void initTestScript() {
        init();
    }

    class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException(String message) {
            super(message);
        }
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionErrorHandling() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Inside script function.");
            throwScriptInterruptedException();
        }, "Ignore"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionWithInnerReplyErrorHandling() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function.");
            assertEquals("No", script.reply("Yes", "No"));
            throwScriptInterruptedException();
            script.say("End of script function.");
        }, "Ignore"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testTwoScriptFunctionsEachWithInnerReplyErrorHandling() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                throwScriptInterruptedException();
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                script.say("End of script function 2");
            }, "Ignore script function 2"));

            script.say("End of script function 1.");
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testThreeScriptFunctionsEachWithInnerReplyErrorHandling() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*1", Debugger.Response.Choose);
        debugger.addResponse("Wow*2", Debugger.Response.Choose);
        debugger.addResponse("Oh*3", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");
                    assertEquals("Oh Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                    throwScriptInterruptedException();
                    script.say("End of script function 3");
                }, "Ignore script function 3"));

                script.say("End of script function 2");
            }, "Ignore script function 2"));

            script.say("End of script function 1.");
        }, "Ignore script function 1"));
        script.say("Resuming main script");

    }

    private static void throwScriptInterruptedException() {
        throw new ScriptInterruptedException();
    }
}
