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
import teaselib.TeaseScript;
import teaselib.test.IntegrationTests;

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesTest extends ShowChoicesAbstractTest {
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

    @Before
    public void initTestScript() {
        init();
    }

    @Test
    public void testSimpleReply() throws Exception {
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("Main script start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("Main script end.");
    }

    @Test
    public void testSingleScriptFunction() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Inside script function.");
            }
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testSingleScriptFunctionWithInnerReply() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function.");
                assertEquals("No", script.reply("Yes", "No"));
                script.say("End of script function.");
            }
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testTwoScriptFunctionsEachWithInnerReply() throws Exception {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
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
                        script.say("End of script function 2");

                    }
                }, "Stop script function 2"));

                script.say("End of script function 1.");

            }
        }, "Stop script function 1"));
        script.say("Resuming main script");
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() throws Exception {
        debugger.addResponse("Stop*", Debugger.Response.Ignore);
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
                                script.say("End of script function 3");

                            }
                        }, "Stop script function 3"));

                        script.say("End of script function 2");

                    }
                }, "Stop script function 2"));

                script.say("End of script function 1.");

            }
        }, "Stop script function 1"));
        script.say("Resuming main script");
    }
}
