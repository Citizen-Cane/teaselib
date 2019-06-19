package teaselib.core.speechrecognition;

import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.awaitResult;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.confidence;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.emulateRecognition;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.Test;

import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionSimpleTest {

    private static void assertRecognized(Choices choices, SpeechRecognition sr) throws InterruptedException {
        for (Choice choice : choices) {
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                String emulatedSpeech = choice.phrases.get(0);
                awaitResult(sr, prompt, SpeechRecognitionTestUtils.withoutPunctation(emulatedSpeech),
                        new Prompt.Result(choices.indexOf(choice)));
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    private static void assertRejected(Choices choices, SpeechRecognition sr, String[] rejected)
            throws InterruptedException {
        for (String speech : rejected) {
            SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence,
                    Optional.empty());
            Prompt prompt = new Prompt(choices, Arrays.asList(inputMethod));
            prompt.lock.lockInterruptibly();
            try {
                inputMethod.show(prompt);
                awaitResult(sr, prompt, SpeechRecognitionTestUtils.withoutPunctation(speech), null);
            } finally {
                prompt.lock.unlock();
            }
        }
    }

    @Test
    public void testResourceHandling() {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        sr.close();
    }

    @Test
    public void testSimpleSRSinglePhrase() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("My name is Foobar")));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);

        assertRecognized(choices, sr);

        String[] rejected = { "My name is Foo", "FooBar" };
        assertRejected(choices, sr, rejected);
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStart() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar"),
                new Choice("My name is Foobar")));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        assertRecognized(choices, sr);
        assertRejected(choices, sr, new String[] { "My name is", "FooBar" });
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("My name is Foo, Mam"),
                new Choice("My name is Bar, Mam"), new Choice("My name is Foobar, Mam")));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        assertRecognized(choices, sr);
        assertRejected(choices, sr, new String[] { "My name is Foobar", "Mam" });
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonEnd() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("I have foobar, Mam"),
                new Choice("My name is foobar, Mam"), new Choice("There is Foobar, Mam")));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        assertRecognized(choices, sr);
        assertRejected(choices, sr, new String[] { "My name is foobar", "Mam" });
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonMiddle() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("I have some foobar, Mam"),
                new Choice("My name is foobar, Miss"), new Choice("There is Foobar, Mistress")));
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        assertRecognized(choices, sr);
        assertRejected(choices, sr, new String[] { "My name is Foobar", "Miss" });
    }

    @Test
    public void testSimpleSRirregularPhrases() throws InterruptedException {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        Choices choices = new Choices(Arrays.asList(new Choice(sorry), new Choice(ready), new Choice(haveIt),
                new Choice(ready2), new Choice(ready3)));

        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class);
        SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(sr, confidence, Optional.empty());

        emulateRecognition(sr, inputMethod, choices, sorry);
        emulateRecognition(sr, inputMethod, choices, ready);
        emulateRecognition(sr, inputMethod, choices, haveIt);
        emulateRecognition(sr, inputMethod, choices, ready2);
        emulateRecognition(sr, inputMethod, choices, ready3);
    }

}
