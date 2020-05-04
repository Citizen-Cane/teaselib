package teaselib.core.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.speechrecognition.RuleIndicesList;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionTestUtils;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.test.TestScript;

public class SpeechRecognitionInputMethodTest {
    private static Choice choice(String text) {
        return new Choice(text, text);
    }

    @Test
    public void testSpeechRecognitionInputMethodEnUs() throws InterruptedException {
        testSInputMethod(new Choices(Locale.US, Intention.Decide, choice("Yes, Miss"), choice("No, Miss")), "Yes Miss",
                0);
    }

    @Test
    public void testSpeechRecognitionInputMethodEnUk() throws InterruptedException {
        testSInputMethod(new Choices(Locale.ENGLISH, Intention.Decide, choice("Yes, Miss"), choice("No, Miss")),
                "No Miss", 1);
    }

    @Test
    public void testSpeechRecognitionInputMethodDeDe() throws InterruptedException {
        testSInputMethod(new Choices(Locale.GERMAN, Intention.Decide, choice("Jawohl, Meister"),
                choice("Natürlich nicht, Meister")), "Natürlich nicht Meister", 1);
    }

    @Test
    public void testSpeechRecognitionInputMethodFrFr() throws InterruptedException {
        testSInputMethod(new Choices(Locale.FRENCH, Intention.Decide, //
                choice("Qui, Madame"), choice("Non, Madame")), "Qui Madame", 0);
    }

    private static void testSInputMethod(Choices choices, String expected, int resultIndex)
            throws InterruptedException {
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers);) {
            SpeechRecognition sr = recognizers.get(choices.locale);
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));
            SpeechRecognitionTestUtils.awaitResult(inputMethod, sr, prompt, expected, new Prompt.Result(resultIndex));
        }
    }

    @Test
    public void testSimpleRecognition() throws InterruptedException {
        String phrase = "Foobar";
        Choices choices = new Choices(Locale.US, Intention.Decide, new Choice(phrase));

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers();
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers);) {
            Prompt prompt = new Prompt(choices, new InputMethods(inputMethod));

            boolean dismissed;
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                recognizers.get(choices.locale).emulateRecogntion(phrase);
                dismissed = prompt.click.await(5, TimeUnit.SECONDS);
            } finally {
                prompt.lock.unlock();
            }

            Prompt.Result result = prompt.result();
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

            SpeechRecognitionTestUtils.awaitResult(inputMethod, sr, prompt, "Yes Miss", new Prompt.Result(0));
        }
    }

    @Test
    public void testSpeechRecognitionInputMethodStacked() throws InterruptedException {
        try (SpeechRecognizer recognizer = SpeechRecognitionTestUtils.getRecognizers();
                SpeechRecognitionInputMethod inputMethod1 = new SpeechRecognitionInputMethod(recognizer);) {
            Choices choices1 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Foo"));
            SpeechRecognition sr = recognizer.get(choices1.locale);

            Prompt prompt1 = new Prompt(choices1, new InputMethods(inputMethod1));
            prompt1.lock.lockInterruptibly();
            try {
                inputMethod1.show(prompt1);
                assertTrue(sr.isActive());
                sr.emulateRecogntion("Bar");
                assertFalse(prompt1.click.await(1, TimeUnit.SECONDS));
                inputMethod1.dismiss(prompt1);
                assertEquals(Prompt.Result.UNDEFINED, prompt1.result());
                assertFalse(sr.isActive());

                Choices choices2 = new Choices(Locale.ENGLISH, Intention.Confirm, choice("Bar"));
                Prompt prompt2 = new Prompt(choices2, new InputMethods(inputMethod1));
                prompt2.lock.lockInterruptibly();
                try {
                    inputMethod1.show(prompt2);
                    assertTrue(sr.isActive());
                    sr.emulateRecogntion("Bar");
                    assertTrue(prompt2.click.await(1, TimeUnit.SECONDS));
                    assertEquals(new Prompt.Result(0), prompt2.result());
                    assertFalse(sr.isActive());
                } finally {
                    prompt2.lock.unlock();
                }

                inputMethod1.show(prompt1);
                assertTrue(sr.isActive());
                sr.emulateRecogntion("Foo");
                assertTrue(prompt1.click.await(1, TimeUnit.SECONDS));
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
    public void testDistinctValue() {
        Integer[][] values = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 },
                { 2, 5 }, };
        RuleIndicesList indices = new RuleIndicesList(
                Arrays.stream(values).map(Arrays::asList).map(HashSet::new).collect(Collectors.toList()));
        assertEquals(2, indices.singleResult().orElseThrow().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleResultNotPresent() {
        Integer[][] values = { { 0, 1, 2, 3, 4 }, { 1, 2, 4 }, { 2, 4 }, { 2 }, { 0, 2, 3, 4, 5 }, { 2, 5 }, { 2, 5 },
                { 0, 5 } };
        RuleIndicesList indices = new RuleIndicesList(
                Arrays.stream(values).map(Arrays::asList).map(HashSet::new).collect(Collectors.toList()));
        assertEquals(2, indices.singleResult().orElseThrow().intValue());
    }

}
