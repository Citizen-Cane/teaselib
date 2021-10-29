package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
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
public class ShowChoicesTestThreadInterrupted extends ShowChoicesAbstractTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[ITERATIONS][0]);
    }

    @Before
    public void initTestScript() throws IOException {
        init();
    }

    static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException() {
            this("test");
        }

        public TestException(String message) {
            super(message);
        }
    }

    @Test(expected = TestException.class)
    public void testSingleScriptFunctionErrorHandling() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Inside script function.");
            throw new TestException();
        }, "Ignore"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionWithInnerReplyInterrupted() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function.");
            assertEquals("No", script.reply("Yes", "No"));
            interruptScript();
            script.say("End of script function.");
            fail();
        }, "Ignore"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testSingleScriptFunctionWithMainThreadInterrupted() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);

        var mainThread = Thread.currentThread();

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function.");

            debugger.resumeTime();
            mainThread.interrupt();
            script.say("End of script function.");
            script.teaseLib.sleep(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            fail();
        }, "Ignore"));
        fail();
    }

    @Test(expected = TestException.class)
    public void testSingleScriptFunctionWithInnerReplyInterruptedErrorHandling() {
        debugger.addResponse("Ignore", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function.");
            assertEquals("No", script.reply("Yes", "No"));
            interruptScript();
            throw new TestException();
        }, "Ignore"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testTwoScriptFunctionsEachWithInnerReplyInterrupted() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Wow*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");
            assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");
                interruptScript();
                assertEquals("Wow Level 2", script.reply("Wow Level 2", "Oh Level 2"));
                script.say("End of script function 2");
                fail();
            }, "Ignore script function 2"));

            fail();
        }, "Ignore script function 1"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testTwoScriptFunctionsEachWithMainThreadInterrupted() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);

        var mainThread = Thread.currentThread();

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");

                debugger.resumeTime();
                mainThread.interrupt();
                script.say("End of script function 2");
                script.teaseLib.sleep(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                fail();
            }, "Ignore script function 2"));

            fail();
        }, "Ignore script function 1"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testTwoScriptFunctionsEachWith1stScriptFunctionThreadInterrupted() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");

            var firstScriptFunction = Thread.currentThread();

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");

                debugger.resumeTime();
                firstScriptFunction.interrupt();
                script.say("End of script function 2");
                script.teaseLib.sleep(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                fail();
            }, "Ignore script function 2"));

            fail();
        }, "Ignore script function 1"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testThreeScriptFunctionsEachWithInnerReplyInterrupted() {
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
                    interruptScript();
                    script.say("End of script function 3");
                    fail();
                }, "Ignore script function 3"));

                fail();
            }, "Ignore script function 2"));

            fail();
        }, "Ignore script function 1"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testThreeScriptFunctionsEachWithMainThreadInterrupted() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);

        var mainThread = Thread.currentThread();

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");

                    debugger.resumeTime();
                    mainThread.interrupt();
                    script.say("End of script function 3");
                    script.teaseLib.sleep(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    fail();
                }, "Ignore script function 3"));

                fail();
            }, "Ignore script function 2"));

            fail();
        }, "Ignore script function 1"));
        fail();
    }

    @Test(expected = ScriptInterruptedException.class)
    public void testThreeScriptFunctionsEachWith2ndScriptFunctionThreadInterrupted() {
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);

        script.say("In main script.");
        assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
            script.say("Start of script function 1.");

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("Start of script function 2.");

                var secondScriptFunction = Thread.currentThread();

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    script.say("Start of script function 3.");

                    debugger.resumeTime();
                    secondScriptFunction.interrupt();
                    script.say("End of script function 3");
                    script.teaseLib.sleep(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    fail();
                }, "Ignore script function 3"));

                fail();
            }, "Ignore script function 2"));

        }, "Ignore script function 1"));
    }

    private static void fail() {
        Assert.fail("Interrupting top level script function has to end script");
    }

    private static void interruptScript() {
        Thread.currentThread().interrupt();
    }
}
