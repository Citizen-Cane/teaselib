package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.ui.InputMethods;
import teaselib.test.TestScript;

/**
 * @author Citizen-Cane
 *
 */
public class ShowChoicesStabilityTest {

    private static final int ITERATIONS = 10;

    @Test
    public void testDismissStability() throws IOException {
        try (var script = new TestScript(new DebugSetup().withInput())) {
            script.debugger.addResponse("2*", Debugger.Response.Choose);
            script.debugger.addResponse("a*", Debugger.Response.Choose);

            script.say("x.");
            for (int i = 0; i < ITERATIONS; i++) {
                script.reply("1", "2");
                script.say("y.");
                script.reply("a");
                script.say("z.");
            }
            script.say("x.");
        }
    }

    @Test
    public void testScriptFunctionDismissStability() throws IOException {
        try (var script = new TestScript(new DebugSetup().withInput())) {
            script.debugger.addResponse("Ignore*", Debugger.Response.Ignore);
            script.debugger.addResponse("3*", Debugger.Response.Choose);
            script.debugger.addResponse("b*", Debugger.Response.Choose);

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                script.say("x.");
                for (int i = 0; i < ITERATIONS; i++) {
                    script.reply("a", "b");
                    script.say("y.");
                    script.reply("1", "2", "3");
                    script.say("z.");
                }
            }, "Ignore"));
            script.say("x.");
        }
    }

    @Test
    public void testScriptFunctionDismissStabilityWIthUnexpectedRecognition() throws IOException {
        try (var script = new TestScript(new DebugSetup().withInput())) {
            script.debugger.addResponse("Ignore*", Debugger.Response.Ignore);
            script.debugger.addResponse("3*", Debugger.Response.Choose);
            script.debugger.addResponse("b*", Debugger.Response.Choose);

            var speechRecognitionInputMethod = script.teaseLib.globals.get(InputMethods.class).get(SpeechRecognitionInputMethod.class);
            for (int i = 0; i < ITERATIONS; i++) {
                assertThrows(NoSuchElementException.class, () -> speechRecognitionInputMethod.emulateRecogntion("foobar"));
                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    speechRecognitionInputMethod.emulateRecogntion("foobar");
                    script.say("x.");
                    script.reply("a", "b");
                    speechRecognitionInputMethod.emulateRecogntion("b");
                    script.say("y.");
                    script.reply("1", "2", "3");
                    speechRecognitionInputMethod.emulateRecogntion("4");
                    script.say("z.");
                }, "Ignore"));
            }
            script.say("x.");
        }
    }

}
