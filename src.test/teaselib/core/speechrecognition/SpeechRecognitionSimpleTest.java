package teaselib.core.speechrecognition;

import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.assertRecognized;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.assertRejected;

import java.util.Locale;

import org.junit.Test;

import teaselib.core.AudioSync;
import teaselib.core.speechrecognition.implementation.TeaseLibSR;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionSimpleTest {

    @Test
    public void testResourceHandling() {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSR.class, new AudioSync());
        sr.close();
    }

    @Test
    public void testSimpleSRSinglePhrase() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, new Choice("My name is Foobar"));

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "My name is Foo", "FooBar");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo"), //
                new Choice("My name is Bar"), //
                new Choice("My name is Foobar"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "My name is", "FooBar");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo, Mam"), //
                new Choice("My name is Bar, Mam"), //
                new Choice("My name is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "My name is Foobar");
            assertRejected(recognizers, inputMethod, choices, "Mam");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have foobar, Mam"), //
                new Choice("My name is foobar, Mam"), //
                new Choice("There is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "My name is foobar");
            assertRejected(recognizers, inputMethod, choices, "Mam");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonMiddle() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have some foobar, Mam"), //
                new Choice("My name is foobar, Miss"), //
                new Choice("There is Foobar, Mistress"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "My name is Foobar");
            assertRejected(recognizers, inputMethod, choices, "Miss");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesHypothesis() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo, Mam"), //
                new Choice("My name is Bar, Mam"), //
                new Choice("My name is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRejected(recognizers, inputMethod, choices, "My name is");
            assertRejected(recognizers, inputMethod, choices, "My name is Foo");
            assertRejected(recognizers, inputMethod, choices, "My name is Foobar");
        }
    }

    @Test
    public void testSimpleSRirregularPhrases() throws InterruptedException {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice(sorry), new Choice(ready), new Choice(haveIt), new Choice(ready2), new Choice(ready3));

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizers(TeaseLibSR.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(recognizers, inputMethod, choices);
            assertRejected(recognizers, inputMethod, choices, "Yes Miss");
            assertRejected(recognizers, inputMethod, choices, "It's ready");
            assertRejected(recognizers, inputMethod, choices, "I'm Sorry", "Ready, Miss", "Ready");
        }
    }

}
