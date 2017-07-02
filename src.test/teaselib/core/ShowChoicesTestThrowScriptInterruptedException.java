package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesTestThrowScriptInterruptedException {

    static final int ITERATIONS = 1;

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[ITERATIONS][0]);
    }

    class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException(String message) {
            super(message);
        }
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionErrorHandling() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

        debugger.addResponse("Ignore", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Inside script function.");

                // script.completeAll();
                // TODO Blocks when thrown right away
                // -> show() hasn't called yet, or not realized by host input
                // method future task
                // - therefore all dismissed code has been passed, and the
                // prompt is never dismissed
                // - same with RuntimeException

                throwScriptInterruptedException();
            }
        }, "Ignore"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionWithInnerReplyErrorHandling() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

        debugger.addResponse("Ignore", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function.");
                assertEquals("No", script.reply("Yes", "No"));
                throwScriptInterruptedException();
                script.say("End of script function.");
            }
        }, "Ignore"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testTwoScriptFunctionsEachWithInnerReplyErrorHandling() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function 1.");
                assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

                assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
                    @Override
                    public void run() {
                        script.say("Start of script function 2.");
                        throwScriptInterruptedException();
                        assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                        script.say("End of script function 2");

                    }
                }, "Ignore script function 2"));

                script.say("End of script function 1.");

            }
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testThreeScriptFunctionsEachWithInnerReplyErrorHandling() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function 1.");
                assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

                assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
                    @Override
                    public void run() {
                        script.say("Start of script function 2.");
                        assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));

                        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
                            @Override
                            public void run() {
                                script.say("Start of script function 3.");
                                assertEquals("No Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                                throwScriptInterruptedException();
                                script.say("End of script function 3");

                            }

                        }, "Ignore script function 3"));

                        script.say("End of script function 2");

                    }
                }, "Ignore script function 2"));

                script.say("End of script function 1.");

            }
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }

    private static void throwScriptInterruptedException() {
        throw new ScriptInterruptedException();
    }
}
