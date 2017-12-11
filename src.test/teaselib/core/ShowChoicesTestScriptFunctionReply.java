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
import teaselib.core.media.MediaRendererThread;
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

    static class DebugInfiniteDelay extends MediaRendererThread {
        public DebugInfiniteDelay(TeaseLib teaseLib) {
            super(teaseLib);
        }

        @Override
        protected void renderMedia() throws InterruptedException {
            startCompleted();
            mandatoryCompleted();
            allCompleted();

            synchronized (this) {
                wait();
            }
        }

        @Override
        public void completeAll() {
            interrupt();
            super.completeAll();
        }
    }

    @Test
    public void testSingleScriptFunctionDismiss() throws Exception {
        debugger.addResponse("Stop", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals("Stop", script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                script.say("Inside script function.");
            }
        }, "Stop"));
        script.say("Resuming main script");
    }

    @Test
    public void testTwoScriptFunctionsDismiss() throws Exception {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function 1.");
                assertEquals("No Level 1", script.reply("Yes Level 1", "No Level 1"));

                assertEquals("Stop script function 2", script.reply(new ScriptFunction() {
                    @Override
                    public void run() {
                        script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                        script.say("Inside script function 2.");
                    }
                }, "Stop script function 2"));

                script.say("End of script function 1.");
            }
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }

    @Test
    public void testThreeScriptFunctionsEachWithInnerReply() throws Exception {
        debugger.addResponse("No*", Debugger.Response.Choose);
        debugger.addResponse("Ignore*", Debugger.Response.Ignore);
        debugger.addResponse("Stop*", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
            @Override
            public void run() {
                script.say("Start of script function 1.");

                assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {
                    @Override
                    public void run() {
                        script.say("Start of script function 2.");
                        assertEquals("No Level 2", script.reply("No Level 2", "Yes Level 2"));

                        assertEquals("Stop script function 3", script.reply(new ScriptFunction() {
                            @Override
                            public void run() {
                                script.queueRenderer(new DebugInfiniteDelay(script.teaseLib));
                                script.say("Inside script function 3.");
                            }
                        }, "Stop script function 3"));

                        script.say("End of script function 2");
                    }
                }, "Ignore script function 2"));

                script.say("End of script function 1.");
            }
        }, "Ignore script function 1"));
        script.say("Resuming main script");
    }
}
