package teaselib.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.test.IntegrationTests;

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesTestErrorHandling extends ShowChoicesAbstractTest {
    static final String THROW_RIGHT_AT_START = "throw right at start";
    static final String THROW_AFTER_FIRST_QUESTION = "throw after first question";

    final String throwWhen;

    @Parameters(name = "{0}")
    public static Iterable<String> data() {
        List<String> variations = Arrays.asList(THROW_RIGHT_AT_START, THROW_AFTER_FIRST_QUESTION);
        List<String> parameters = new ArrayList<String>(ITERATIONS * variations.size());
        for (int i = 0; i < ITERATIONS; i++) {
            parameters.addAll(variations);
        }
        return parameters;
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

    public ShowChoicesTestErrorHandling(String throwWhen) {
        this.throwWhen = throwWhen;
    }

    @Test(expected = TestException.class)
    public void testSingleScriptFunctionErrorHandling() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                if (throwWhen == THROW_RIGHT_AT_START)
                    throwTestException();
                script.say("Inside script function.");
                script.completeAll();
                if (throwWhen == THROW_AFTER_FIRST_QUESTION)
                    throwTestException();
            }
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test(expected = TestException.class)
    public void testSingleScriptFunctionWithInnerReplyErrorHandling() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function.");
                if (throwWhen == THROW_RIGHT_AT_START)
                    throwTestException();
                assertEquals("No", script.reply("Yes", "No"));
                if (throwWhen == THROW_AFTER_FIRST_QUESTION)
                    throwTestException();
                script.say("End of script function.");
            }
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test(expected = TestException.class)
    public void testTwoScriptFunctionsEachWithInnerReplyErrorHandling() throws Exception {
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
                        if (throwWhen == THROW_RIGHT_AT_START)
                            throwTestException();
                        assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                        script.say("End of script function 2");
                        script.completeAll();
                        if (throwWhen == THROW_AFTER_FIRST_QUESTION)
                            throwTestException();
                    }
                }, "Stop script function 2"));

                script.say("End of script function 1.");

            }
        }, "Stop script function 1"));
        script.say("Resuming main script");
    }

    @Test(expected = TestException.class)
    public void testThreeScriptFunctionsEachWithInnerReplyErrorHandling() throws Exception {
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
                                if (throwWhen == THROW_RIGHT_AT_START)
                                    throwTestException();
                                assertEquals("No Level 3", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                                if (throwWhen == THROW_AFTER_FIRST_QUESTION)
                                    throwTestException();
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

    private void throwTestException() {
        throw new TestException("in script function");
    }
}
