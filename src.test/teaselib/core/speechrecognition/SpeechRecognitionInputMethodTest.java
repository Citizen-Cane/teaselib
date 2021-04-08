package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

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
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers);) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            SpeechRecognitionTestUtils.awaitResult(prompt, inputMethod, expected, new Prompt.Result(resultIndex));
        }
    }

    @Test
    public void testSimpleRecognition() throws InterruptedException {
        String phrase = "Foobar";
        Choices choices = new Choices(Locale.US, Intention.Decide, new Choice(phrase));

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers);) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));

            Prompt.Result result;
            boolean dismissed;
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                recognizers.get(choices.locale).emulateRecogntion(phrase);
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
    public void testShowDismissStability() throws InterruptedException, IOException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                choice("Yes, Miss"), choice("No, Miss"));
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration(setup);
        try (SpeechRecognizer recognizer = new SpeechRecognizer(config);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer);) {
            SpeechRecognition sr = recognizer.get(choices.locale);
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));

            for (int i = 0; i < 10; i++) {
                prompt.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt);
                    sr.emulateRecogntion("Foobar");
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
        try (SpeechRecognizer recognizer = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRGS.Relaxed.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizer);) {
            Choices choices1 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Foo"));
            SpeechRecognition sr = recognizer.get(choices1.locale);

            Prompt prompt1 = new Prompt(choices1, new InputMethods(inputMethod));
            prompt1.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt1);
                assertTrue(sr.isActive());
                sr.emulateRecogntion("Bar");
                assertFalse("Bar unexpected", prompt1.click.await(1, TimeUnit.SECONDS));
                inputMethod.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());
                assertFalse(sr.isActive());

                Choices choices2 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Bar"));
                Prompt prompt2 = new Prompt(choices2, new InputMethods(inputMethod));
                prompt2.lock.lockInterruptibly();
                try {
                    inputMethod.show(prompt2);
                    assertTrue(sr.isActive());
                    sr.emulateRecogntion("Bar");
                    assertTrue("Bar expected", prompt2.click.await(1, TimeUnit.SECONDS));
                    assertEquals(new Prompt.Result(0), prompt2.result());
                    assertFalse(sr.isActive());
                } finally {
                    prompt2.lock.unlock();
                }

                inputMethod.show(prompt1);
                assertTrue(sr.isActive());
                sr.emulateRecogntion("Bar");
                assertFalse("Bar unexpected", prompt1.click.await(1, TimeUnit.SECONDS));
                inputMethod.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());
                assertFalse(sr.isActive());

                inputMethod.show(prompt1);
                assertTrue(sr.isActive());
                sr.emulateRecogntion("Foo");
                assertTrue("Foo expected", prompt1.click.await(1, TimeUnit.SECONDS));
                assertEquals(new Prompt.Result(0), prompt1.result());
                assertFalse(sr.isActive());
            } finally {
                prompt1.lock.unlock();
            }
        }

    }

    @Test
    public void testSpeechRecognitionInputMethodStackedScript() {
        TestScript script = TestScript.getOne(new DebugSetup().withInput());
        script.debugger.detach();

        InputMethods inputMethods = script.teaseLib.globals.get(InputMethods.class);
        SpeechRecognitionInputMethod speechRecognition = inputMethods.get(SpeechRecognitionInputMethod.class);

        assertEquals("Foo", script.reply(() -> {
            assertEquals("Bar", script.reply(() -> {
                speechRecognition.emulateRecogntion("Bar");
                script.sleep(1, TimeUnit.SECONDS);
            }, "Bar"));
            speechRecognition.emulateRecogntion("Foo");
            script.sleep(1, TimeUnit.SECONDS);
        }, "Foo"));
    }

}
