package teaselib.core.ui;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.Confidence;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionTestUtils;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.test.TestScript;

public class SpeechRecognitionInputMethodTest {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    private static final Confidence confidence = Confidence.High;

    @Test
    public void testSpeechRecognitionInputMethod_EN_US() throws InterruptedException, IOException {
        testSInputMethod("en-us", new Choices(choice("Yes, Miss"), choice("No, Miss")), "Yes, Miss");
    }

    @Test
    public void testSpeechRecognitionInputMethod_EN_UK() throws InterruptedException, IOException {
        testSInputMethod("en-uk", new Choices(choice("Yes, Miss"), choice("No, Miss")), "Yes, Miss");
    }

    @Test
    public void testSpeechRecognitionInputMethod_DE_DE() throws InterruptedException, IOException {
        testSInputMethod("de-de", new Choices(choice("Jawohl, Meister"), choice("Natürlich nicht, Meister")),
                "Natürlich nicht, Meister");
    }

    @Test
    public void testSpeechRecognitionInputMethod_FR_FR() throws InterruptedException, IOException {
        testSInputMethod("fr-fr", new Choices(choice("Qui, Madame"), choice("Non, Madame")), "Qui, Madame");
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
            awaitRecognition(sr, inputMethod, new Prompt(choices, Arrays.asList(inputMethod)), expected);
        }
    }

    @Test
    public void testShowDismissStability() throws InterruptedException, IOException {
        Choices choices = new Choices(choice("Yes, Miss"), choice("No, Miss"));
        DebugSetup setup = new DebugSetup().withInput();
        Configuration config = new Configuration(setup);
        try (SpeechRecognizer speechRecognizer = new SpeechRecognizer(config);) {
            SpeechRecognition sr = speechRecognizer.get(Locale.US);
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));

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
            awaitRecognition(sr, inputMethod, prompt, "Yes, Miss");
        }
    }

    private static void awaitRecognition(SpeechRecognition sr, SpeechRecognitionInputMethod inputMethod, Prompt prompt,
            String expected) throws InterruptedException {
        prompt.lock.lockInterruptibly();
        try {
            inputMethod.show(prompt);
            sr.emulateRecogntion(SpeechRecognitionTestUtils.withoutPunctation(expected));
            assertTrue(prompt.click.await(1, TimeUnit.SECONDS));
            inputMethod.dismiss(prompt);
        } finally {
            prompt.lock.unlock();
        }
        List<String> all = prompt.choices.toPhrases().stream().map(phrases -> phrases.get(0)).collect(toList());
        assertEquals(all.indexOf(expected), prompt.result().elements.get(0).intValue());
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
                assertFalse(prompt1.click.await(1, TimeUnit.SECONDS));
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
                    assertTrue(prompt2.click.await(1, TimeUnit.SECONDS));
                    inputMethod2.dismiss(prompt2);
                    assertEquals(new Prompt.Result(0), prompt2.result());
                } finally {
                    prompt2.lock.unlock();
                }

                inputMethod1.show(prompt1);
                sr.emulateRecogntion("Foo");
                assertTrue(prompt1.click.await(1, TimeUnit.SECONDS));
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

    @Test
    public void testCommonDistinctValue() {
        Integer[][] array = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 }, { 2, 5 }, };
        List<Set<Integer>> values = Arrays.stream(array).map(Arrays::asList).map(HashSet::new)
                .collect(Collectors.toList());
        assertEquals(2, SpeechRecognitionInputMethod.getCommonDistinctValue(values).orElseThrow().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testCommonDistinctValueNotPresent() {
        Integer[][] array = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 }, { 2, 5 },
                { 0, 5 } };
        List<Set<Integer>> values = Arrays.stream(array).map(Arrays::asList).map(HashSet::new)
                .collect(Collectors.toList());
        assertEquals(2, SpeechRecognitionInputMethod.getCommonDistinctValue(values).orElseThrow().intValue());
    }

}
