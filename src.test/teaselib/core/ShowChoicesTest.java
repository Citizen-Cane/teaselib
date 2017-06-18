package teaselib.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.test.TestScript;

public class ShowChoicesTest {
    @Test
    public void testShowWithSingleScriptFunction() throws Exception {
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

    @Test
    public void testShowWithTwoScriptFunctions() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

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
    public void testShowWithThreeScriptFunctions() throws Exception {
        final TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;

        debugger.freezeTime();

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
                                assertEquals("Wow Level 2", script.reply("No Level 3", "Wow Level 3", "Oh Level 3"));
                                script.say("End of script function 3");

                            }
                        }, "Stop script function 2"));

                        script.say("End of script function 2");

                    }
                }, "Stop script function 2"));

                script.say("End of script function 1.");

            }
        }, "Stop script function 1"));
        script.say("Resuming main script");
    }
}
