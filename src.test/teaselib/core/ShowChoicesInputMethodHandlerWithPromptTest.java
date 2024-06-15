package teaselib.core;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.debug.DebugHost;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class ShowChoicesInputMethodHandlerWithPromptTest {

    ShowChoicesAbstractTest.RunnableTestScript script;
    Debugger debugger;
    AtomicInteger count;
    DebugHost host;
    TeaseLib teaseLib;

    @BeforeEach
    public void init() throws IOException {
        host = new DebugHost();
        teaseLib = new TeaseLib(host, new DebugSetup());
        Actor actor = TestScript.newActor(Gender.Masculine);
        ResourceLoader resourceLoader = new ResourceLoader(this.getClass());

        script = new ShowChoicesAbstractTest.RunnableTestScript(teaseLib, resourceLoader, actor, "foobar") {
            @Override
            public void run() {
                // Ignore
            }
        };

        ShowChoicesAbstractTest.RunnableTestScript debugInputMethodHandler = new ShowChoicesAbstractTest.RunnableTestScript(
                script) {
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

    @AfterEach
    public void cleanup() {
        teaseLib.close();
        host.close();
    }

    @Test
    public void testHandlerSingleInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        Assertions.assertEquals(1, count.get());
    }

    @Test
    public void testHandlerDoubleInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void testHandlerTripleInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No", "Maybe"));
        script.say("End.");

        Assertions.assertEquals(3, count.get());
    }

    @Test
    public void testHandlerQuadInvocation() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("Later", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No", "Maybe", "Later"));
        script.say("End.");

        Assertions.assertEquals(4, count.get());
    }

    @Test
    public void testHandlerSingleInvocationRealTime() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);
        debugger.resumeTime();

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        Assertions.assertEquals(1, count.get());
    }

    @Test
    public void testHandlerDoubleInvocationRealTime() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);
        debugger.resumeTime();

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No"));
        script.say("End.");

        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void testHandlerTripleInvocationRealTime() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);
        debugger.resumeTime();

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No", "Maybe"));
        script.say("End.");

        Assertions.assertEquals(3, count.get());
    }

    @Test
    public void testHandlerQuadInvocationRealTime() {
        debugger.addResponse("No", Debugger.Response.Invoke);
        debugger.addResponse("Yes", Debugger.Response.Invoke);
        debugger.addResponse("Maybe", Debugger.Response.Invoke);
        debugger.addResponse("Later", Debugger.Response.Invoke);
        debugger.addResponse("DebugConfirm", Debugger.Response.Choose);
        debugger.resumeTime();

        script.say("Start.");
        Assertions.assertEquals("No", script.reply("Yes", "No", "Maybe", "Later"));
        script.say("End.");

        Assertions.assertEquals(4, count.get());
    }
}
