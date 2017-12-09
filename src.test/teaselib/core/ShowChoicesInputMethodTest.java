package teaselib.core;

import static org.junit.Assert.*;

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
public class ShowChoicesInputMethodTest {
    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[1][0]);
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

        script = new TeaseScript(teaseLib, resourceLoader, actor, "foobar") {
            @Override
            public void run() {
                // Ignore
            }
        };

        TeaseScript debugInputMethodHandler = new TeaseScript(script) {
            @Override
            public void run() {
                say("In Debug Handler");
                reply("DebugConfirm");
                count.incrementAndGet();
            }
        };

        debugger = new Debugger(teaseLib, debugInputMethodHandler);
        count = new AtomicInteger(0);
    }

    @Test
    public void testSingleReply() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(1, count.get());
    }

    @Test
    public void testDoubleReply() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        assertEquals(2, count.get());
    }

    @Test
    public void testTripleReply() throws Exception {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        assertEquals("No", script.reply("Yes", "No", "Maybe"));
        script.say("End.");

        assertEquals(3, count.get());
    }
}
