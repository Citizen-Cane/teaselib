package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.TeaseScript;
import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugPersistence;
import teaselib.test.DebugSetup;
import teaselib.test.IntegrationTests;

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesInputMethodHandlerTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[ShowChoicesAbstractTest.ITERATIONS][0]);
    }

    private abstract class RunnableTestScript extends TeaseScript implements Runnable {
        private RunnableTestScript(Script script) {
            super(script);
        }

        public RunnableTestScript(TeaseLib teaseLib, ResourceLoader resourceLoader, Actor actor, String namespace) {
            super(teaseLib, resourceLoader, actor, namespace);
        }
    }

    class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException(String message) {
            super(message);
        }
    }

    TeaseScript script;
    Debugger debugger;
    AtomicInteger count;

    @Before
    public void init() throws IOException {
        TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugPersistence(), new DebugSetup());
        Actor actor = teaseLib.getDominant(Gender.Masculine, Locale.US);
        ResourceLoader resourceLoader = new ResourceLoader(this.getClass());

        script = new RunnableTestScript(teaseLib, resourceLoader, actor, "foobar") {
            @Override
            public void run() { // Ignore
            }
        };

        RunnableTestScript debugInputMethodHandler = new RunnableTestScript(script) {
            @Override
            public void run() {
                say("In Debug Handler");
                count.incrementAndGet();
            }
        };

        debugger = new Debugger(teaseLib, debugInputMethodHandler);
        count = new AtomicInteger(0);
    }

    @Test
    public void testHandlerSingleInvocation() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(1, count.get());
    }

    @Test
    public void testHandlerDoubleInvocation() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(2, count.get());
    }

    @Test
    public void testHandlerTripleInvocation() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No", "Maybe"));
        script.say("End.");

        assertEquals(3, count.get());
    }

    @Test
    public void testHandlerQuadInvocation() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("Later", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No", "Maybe", "Later"));
        script.say("End.");

        assertEquals(4, count.get());
    }
}
