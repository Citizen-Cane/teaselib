package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
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
import teaselib.core.configuration.DebugSetup;
import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugPersistence;
import teaselib.test.IntegrationTests;
import teaselib.test.TestScript;

// TODO sometimes blocks because timeAdvanceListener locks prompt in render thread to invoke a second handler
// - find out who has locked the prompt

@Category(IntegrationTests.class)
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShowChoicesInputMethodHandlerTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[ShowChoicesAbstractTest.ITERATIONS][0]);
    }

    private abstract class RunnableTestScript extends TeaseScript implements Runnable {
        RunnableTestScript(Script script) {
            super(script);
        }

        public RunnableTestScript(TeaseLib teaseLib, ResourceLoader resourceLoader, Actor actor, String namespace) {
            super(teaseLib, resourceLoader, actor, namespace);
        }
    }

    TeaseScript script;
    Debugger debugger;
    AtomicInteger count;
    DebugHost host;
    TeaseLib teaseLib;

    @Before
    public void init() throws IOException {
        host = new DebugHost();
        teaseLib = new TeaseLib(host, new DebugPersistence(), new DebugSetup());
        Actor actor = TestScript.newActor(Gender.Masculine);
        ResourceLoader resourceLoader = new ResourceLoader(this.getClass());

        script = new RunnableTestScript(teaseLib, resourceLoader, actor, "foobar") {
            @Override
            public void run() { // Ignore
            }
        };

        RunnableTestScript debugInputMethodHandler = new RunnableTestScript(script) {
            @Override
            public void run() {
                say("DebugInputMethod Handler called " + count.incrementAndGet() + " times.");
            }
        };

        debugger = new Debugger(teaseLib, debugInputMethodHandler);
        count = new AtomicInteger(0);
    }

    @After
    public void cleanup() {
        teaseLib.close();
        host.close();
    }

    @Test
    public void testHandlerSingleInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(1, count.get());
    }

    @Test
    public void testHandlerDoubleInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(2, count.get());
    }

    @Test
    public void testHandlerTripleInvocation() {
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
    public void testHandlerQuadInvocation() {
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
