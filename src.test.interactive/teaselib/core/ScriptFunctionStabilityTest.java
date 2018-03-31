/**
 * 
 */
package teaselib.core;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import teaselib.TeaseScript;
import teaselib.test.DebugSetup;
import teaselib.test.TestScript;

public class ScriptFunctionStabilityTest {
    protected static final DebugSetup DEBUG_SETUP = new DebugSetup();// .withOutput().withInput();
    protected static final int ITERATIONS = 10000;

    protected TestScript script;
    protected Debugger debugger;

    @Before
    public void initTestScript() {
        script = TestScript.getOne(DEBUG_SETUP);
        debugger = script.debugger;
        debugger.freezeTime();
    }

    class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException(String message) {
            super(message);
        }
    }

    @Test
    public void testSingleScriptFunctionDismiss() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Choose);

        for (int i = 0; i < ITERATIONS; i++) {
            script.say("In main script.");
            assertEquals("Stop", script.reply(() -> {
                script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                script.say("Inside script function.");
            }, "Stop"));
            script.say("Resuming main script");
        }
    }

    @Test
    public void testTwoScriptFunctionsDismiss() throws Exception {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        for (int i = 0; i < ITERATIONS; i++) {
            script.say("In main script.");
            assertEquals(TeaseScript.Timeout, script.reply(() -> {
                script.say("Start of script function 1.");
                assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

                assertEquals("Stop script function 2", script.reply(() -> {
                    script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                    script.say("Inside script function 2.");
                }, "Stop script function 2"));

                script.say("End of script function 1.");
            }, "Ignore script function 1"));
            script.say("Resuming main script");
        }
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() throws Exception {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        for (int i = 0; i < ITERATIONS; i++) {
            script.say("In main script.");
            assertEquals(TeaseScript.Timeout, script.reply(() -> {
                script.say("Start of script function 1.");

                assertEquals(TeaseScript.Timeout, script.reply(() -> {
                    script.say("Start of script function 2.");
                    assertEquals("No Level 2", script.reply("No Level 2", "Yes Level 2"));

                    assertEquals("Stop script function 3", script.reply(() -> {
                        script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                        script.say("Inside script function 3.");
                    }, "Stop script function 3"));

                    script.say("End of script function 2");
                }, "Ignore script function 2"));

                script.say("End of script function 1.");
            }, "Ignore script function 1"));
            script.say("Resuming main script");
        }
    }
}
