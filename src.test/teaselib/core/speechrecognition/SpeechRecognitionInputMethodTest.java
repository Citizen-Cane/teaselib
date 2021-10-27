package teaselib.core.speechrecognition;

import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.*;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.ScriptFunction;
import teaselib.core.AudioSync;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils;
import teaselib.core.speechrecognition.sapi.TeaseLibSRGS;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.InputMethods;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;
import teaselib.test.TestScript;

public class SpeechRecognitionInputMethodTest {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    @Test
    public void testSpeechRecognitionInputMethodEnUs() throws InterruptedException {
        test(new Choices(Locale.US, Intention.Decide, choice("Yes, Miss"), choice("No, Miss")), "Yes Miss", 0);
    }

    @Test
    public void testSpeechRecognitionInputMethodEnUk() throws InterruptedException {
        test(new Choices(Locale.ENGLISH, Intention.Decide, choice("Yes, Miss"), choice("No, Miss")), "No Miss", 1);
    }

    @Test
    public void testSpeechRecognitionInputMethodDeDe() throws InterruptedException {
        test(new Choices(Locale.GERMAN, Intention.Decide, choice("Jawohl, Meister"),
                choice("Natürlich nicht, Meister")), "Natürlich nicht Meister", 1);
    }

    @Test
    public void testSpeechRecognitionInputMethodFrFr() throws InterruptedException {
        test(new Choices(Locale.FRENCH, Intention.Decide, //
                choice("Qui, Madame"), choice("Non, Madame")), "Qui Madame", 0);
    }

    private static void test(Choices choices, String expected, int resultIndex) throws InterruptedException {
        try (InputMethods inputMethods = new InputMethods(getInputMethod(TeaseLibSRGS.Relaxed.class))) {
            SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
            Prompt prompt = new Prompt(choices, inputMethods);
            SpeechRecognitionTestUtils.awaitResult(prompt, inputMethod, expected, new Prompt.Result(resultIndex));
        }
    }

    @Test
    public void testSimpleRecognition() throws InterruptedException {
        String phrase = "Foobar";
        Choices choices = new Choices(Locale.US, Intention.Decide, new Choice(phrase));

        try (InputMethods inputMethods = new InputMethods(getInputMethod(TeaseLibSRGS.Relaxed.class))) {
            SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
            Prompt prompt = new Prompt(choices, inputMethods);

            Prompt.Result result;
            boolean dismissed;
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                inputMethod.emulateRecogntion(phrase);
                dismissed = prompt.click.await(5, TimeUnit.SECONDS);
                result = prompt.result();
            } finally {
                prompt.lock.unlock();
            }

            assertEquals(phrase, new Prompt.Result(0), result);
            assertTrue("Expected dismissed prompt", dismissed);
        }
    }

    @Test
    public void testEventHandlingIsStoppedBetweenRecognitions() throws InterruptedException {
        String foo = "Foo foo";
        String bar = "Bar bar";

        try (InputMethods inputMethods = new InputMethods(getInputMethod(TeaseLibSRGS.Relaxed.class))) {
            SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);

            {
                Choices choices = new Choices(Locale.US, Intention.Decide, new Choice(foo), new Choice(bar));
                Prompt prompt = new Prompt(choices, inputMethods);

                Prompt.Result result;
                boolean dismissed;
                prompt.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt);
                    inputMethod.emulateRecogntion(bar);
                    dismissed = prompt.click.await(5, TimeUnit.SECONDS);
                    result = prompt.result();

                    assertEquals(bar, new Prompt.Result(1), result);
                    assertTrue("Expected dismissed prompt", dismissed);
                } finally {
                    inputMethod.dismiss(prompt);
                    prompt.lock.unlock();
                }

            }

            // SAPI text emulation is different from actual recognition
            SpeechRecognition speechRecognition = inputMethod.speechRecognizer.get(Locale.US);
            speechRecognition.emulateRecogntion("bar");

            {
                Choices choices = new Choices(Locale.US, Intention.Decide, new Choice(foo));
                Prompt prompt = new Prompt(choices, inputMethods);
                Prompt.Result result;
                boolean dismissed;
                prompt.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt);
                    // inputMethod.emulateRecogntion(bar);
                    dismissed = prompt.click.await(5, TimeUnit.SECONDS);
                    result = prompt.result();

                    assertEquals(Prompt.Result.UNDEFINED, result);
                    assertFalse("Expected dismissed prompt", dismissed);
                } finally {
                    inputMethod.dismiss(prompt);
                    prompt.lock.unlock();
                }

            }

        }
    }

    @Test
    public void testShowDismissStability() throws InterruptedException, IOException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                choice("Yes, Miss"), choice("No, Miss"));
        DebugSetup setup = new DebugSetup().withInput();
        try (Configuration config = new Configuration(setup);
                InputMethods inputMethods = new InputMethods(
                        new SpeechRecognitionInputMethod(config, new AudioSync()));) {
            SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
            Prompt prompt = new Prompt(choices, inputMethods);

            for (int i = 0; i < 10; i++) {
                prompt.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt);
                    inputMethod.emulateRecogntion("Foobar");
                    inputMethod.dismiss(prompt);
                } finally {
                    prompt.lock.unlock();
                }
            }

            SpeechRecognitionTestUtils.awaitResult(prompt, inputMethod, "Yes Miss", new Prompt.Result(0));
        }
    }

    @Test
    public void testSpeechRecognitionInputMethodStacked() throws InterruptedException {
        try (InputMethods inputMethods = new InputMethods(getInputMethod(TeaseLibSRGS.Relaxed.class))) {
            SpeechRecognitionInputMethod inputMethod = inputMethods.get(SpeechRecognitionInputMethod.class);
            Choices choices1 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Foo"));

            Prompt prompt1 = new Prompt(choices1, inputMethods);
            prompt1.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt1);
                inputMethod.emulateRecogntion("Bar");
                assertFalse("Bar unexpected", prompt1.click.await(1, TimeUnit.SECONDS));
                inputMethod.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());

                Choices choices2 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Bar"));
                Prompt prompt2 = new Prompt(choices2, inputMethods);
                prompt2.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt2);
                    inputMethod.emulateRecogntion("Bar");
                    assertTrue("Bar expected", prompt2.click.await(1, TimeUnit.SECONDS));
                    assertEquals(new Prompt.Result(0), prompt2.result());
                } finally {
                    prompt2.lock.unlock();
                }

                inputMethod.show(prompt1);
                inputMethod.emulateRecogntion("Bar");
                assertFalse("Bar unexpected", prompt1.click.await(1, TimeUnit.SECONDS));
                inputMethod.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());

                inputMethod.show(prompt1);
                inputMethod.emulateRecogntion("Foo");
                assertTrue("Foo expected", prompt1.click.await(1, TimeUnit.SECONDS));
                assertEquals(new Prompt.Result(0), prompt1.result());
            } finally {
                prompt1.lock.unlock();
            }
        }
    }

    @Test
    public void testSpeechRecognitionStackedScriptFucntions() throws IOException {
        try (TestScript script = new TestScript(new DebugSetup().withInput())) {
            script.debugger.detach();

            InputMethods inputMethods = script.teaseLib.globals.get(InputMethods.class);
            SpeechRecognitionInputMethod speechRecognition = inputMethods.get(SpeechRecognitionInputMethod.class);

            assertEquals("Foo", script.reply(() -> {

                assertEquals("Bar", script.reply(() -> {
                    speechRecognition.emulateRecogntion("Bar");
                    script.sleep(2, TimeUnit.SECONDS);
                }, "Bar"));

                speechRecognition.emulateRecogntion("Foo");
                script.sleep(2, TimeUnit.SECONDS);
            }, "Foo"));
        }
    }

    @Test
    public void testSpeechRecognitionTimeoutStackedScriptFucntions() throws IOException {
        try (TestScript script = new TestScript(new DebugSetup().withInput())) {
            script.debugger.detach();

            InputMethods inputMethods = script.teaseLib.globals.get(InputMethods.class);
            SpeechRecognitionInputMethod speechRecognition = inputMethods.get(SpeechRecognitionInputMethod.class);

            assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {

                assertEquals(ScriptFunction.TimeoutString, script.reply(() -> {
                    speechRecognition.emulateRecogntion("Blub");
                    script.sleep(2, TimeUnit.SECONDS);
                }, "Bar"));

                speechRecognition.emulateRecogntion("Blub");
                script.sleep(2, TimeUnit.SECONDS);
            }, "Foo"));
        }
    }

}
