package teaselib.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

public class ShowChoicesTest {
    @Test
    public void testShow() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

        debugger.addResponse("Stop", Debugger.Response.Ignore);
        debugger.addResponse("No", Debugger.Response.Choose);

        script.say("In main script.");
        assertEquals(TeaseScript.Timeout, script.reply(new ScriptFunction() {

            @Override
            public void run() {
                script.say("Inside script function.");
                assertEquals("No", script.reply("Yes", "No"));
                script.say("End of script function");

            }
        }, "Stop"));
        script.say("Resuming main script");
    }
}
