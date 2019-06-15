package teaselib.core.ui;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.test.TestScript;

public class SpeechRecognitionInputMethodTest {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    private static final Confidence confidence = Confidence.High;

    @Test
    public void testSpeechRecognitionInputMethod_EN_US() throws InterruptedException, IOException {
        testSInputMethod("en-us", new Choices(choice("Yes, Miss"), choice("No, Miss")), "Yes Miss");
    }

    @Test
    public void testSpeechRecognitionInputMethod_EN_UK() throws InterruptedException, IOException {
        testSInputMethod("en-uk", new Choices(choice("Yes, Miss"), choice("No, Miss")), "Yes Miss");
    }

    @Test
    public void testSpeechRecognitionInputMethod_DE_DE() throws InterruptedException, IOException {
        testSInputMethod("de-de", new Choices(choice("Jawohl, Meister"), choice("Natürlich nicht, Meister")),
                "Jawohl Meister");
    }

    @Test
    public void testSpeechRecognitionInputMethod_FR_FR() throws InterruptedException, IOException {
        testSInputMethod("fr-fr", new Choices(choice("Qui, Madame"), choice("Non, Madame")), "Qui Madame");
    }

    private static void testSInputMethod(String locale, Choices choices, String expected)
            throws IOException, InterruptedException {
        // avoid teaselib.core.jni.COMException: COM-Error 0x80045052 - langCode of recognizer and srgs differ
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration(setup);
        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(config);) {
            SpeechRecognition sr = speechRecognizer.get(new Locale(locale));
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                sr.emulateRecogntion(expected);
                prompt.click.await(1, TimeUnit.SECONDS);
                inputMethod.dismiss(prompt);
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    @Test
    public void testSpeechRecognitionInputMethodStacked() throws InterruptedException, IOException {
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration(setup);
        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(config);) {
            SpeechRecognition sr = speechRecognizer.get(new Locale("en-us"));

            SpeechRecognitionInputMethod inputMethod1 = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Choices choices1 = new Choices(choice("Foo"));
            Prompt prompt1 = new Prompt(choices1, Arrays.asList(inputMethod1));
            prompt1.lock.lockInterruptibly();
            try {
                inputMethod1.show(prompt1);
                sr.emulateRecogntion("Bar");
                prompt1.click.await(1, TimeUnit.SECONDS);
                inputMethod1.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());

                SpeechRecognitionInputMethod inputMethod2 = new SpeechRecognitionInputMethod(sr, confidence,
                        Optional.empty());
                Choices choices2 = new Choices(choice("Bar"));
                Prompt prompt2 = new Prompt(choices2, Arrays.asList(inputMethod2));
                prompt2.lock.lockInterruptibly();
                try {
                    inputMethod2.show(prompt2);
                    sr.emulateRecogntion("Bar");
                    prompt2.click.await(1, TimeUnit.SECONDS);
                    inputMethod2.dismiss(prompt2);
                    assertEquals(new Prompt.Result(0), prompt2.result());
                } finally {
                    prompt2.lock.unlock();
                }

                inputMethod1.show(prompt1);
                sr.emulateRecogntion("Foo");
                prompt1.click.await(1, TimeUnit.SECONDS);
                assertEquals(new Prompt.Result(0), prompt1.result());
            } finally {
                prompt1.lock.unlock();
            }
        }
    }

    @Test
    public void testSpeechRecognitionInputMethodStackedScript() {
        TestScript script = TestScript.getOne(new DebugSetup().withInput());
        script.debugger.detach();

        try (SpeechRecognizer speechRecognizer = script.teaseLib.globals.get(SpeechRecognizer.class)) {
            SpeechRecognition sr = speechRecognizer.get(script.actor.locale());

            assertEquals("Foo", script.reply(() -> {
                assertEquals("Bar", script.reply(() -> {
                    sr.emulateRecogntion("Bar");
                    script.sleep(1, TimeUnit.SECONDS);
                }, "Bar"));
                sr.emulateRecogntion("Foo");
                script.sleep(1, TimeUnit.SECONDS);
            }, "Foo"));
        }
    }
}
