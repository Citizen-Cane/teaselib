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

import teaselib.TeaseScript;
import teaselib.test.IntegrationTests;

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesTestScriptFunctionReply extends ShowChoicesAbstractTest {
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

    @Test
    public void testSingleScriptFunctionDismiss() {
        debugger.addResponse("Stop", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals("Stop", script.reply(() -> {
            script.scriptRenderer.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
            script.say("Inside script function.");
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testTwoScriptFunctionsDismiss() {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals("Stop script function 2", script.reply(() -> {
                script.scriptRenderer.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                script.say("Inside script function 2.");
            }, "Stop script function 2"));

            script.say("End of script function 1.");
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(() -> {
            script.say("Start of script function 1.");

            assertEquals(TeaseScript.Timeout, script.reply(() -> {
                script.say("Start of script function 2.");
                assertEquals("No Level 2", script.reply("No Level 2", "Yes Level 2"));

                assertEquals("Stop script function 3", script.reply(() -> {
                    script.scriptRenderer.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                    script.say("Inside script function 3.");
                }, "Stop script function 3"));

                script.say("End of script function 2");
            }, "Ignore script function 2"));

            script.say("End of script function 1.");
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }
}
