package teaselib.core.speechrecognition.sapi;

import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.as;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRecognized;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRecognizedAsHypothesis;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.assertRejected;

import java.util.Locale;

import org.junit.Test;

import teaselib.core.AudioSync;
import teaselib.core.speechrecognition.SpeechRecognition;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.speechrecognition.SpeechRecognizer;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionSimpleTest {

    @Test
    public void testResourceHandling() {
        SpeechRecognition sr = new SpeechRecognition(Locale.ENGLISH, TeaseLibSRSimple.class, new AudioSync());
        sr.close();
    }

    @Test
    public void testSimpleSRSinglePhrase() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, new Choice("My name is Foobar"));

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(inputMethod, choices);
            assertRejected(inputMethod, choices, "My name is Foo", "FooBar");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo"), //
                new Choice("My name is Bar"), //
                new Choice("My name is Foobar"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(inputMethod, choices);
            assertRejected(inputMethod, choices, "My name is", "FooBar");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo, Mam"), //
                new Choice("My name is Bar, Mam"), //
                new Choice("My name is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(inputMethod, choices);
            assertRecognizedAsHypothesis(inputMethod, choices, "My name is Foobar", new Prompt.Result(2));
            assertRejected(inputMethod, choices, "Mam");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have foobar, Mam"), //
                new Choice("My name is foobar, Mam"), //
                new Choice("There is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(inputMethod, choices);
            assertRecognizedAsHypothesis(inputMethod, choices, "My name is Foobar", new Prompt.Result(1));
            assertRejected(inputMethod, choices, "Mam");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesCommonMiddle() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I have some foobar, Mam"), //
                new Choice("My name is foobar, Miss"), //
                new Choice("There is Foobar, Mistress"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRecognized(inputMethod, choices);
            assertRecognizedAsHypothesis(inputMethod, choices, "My name is Foobar", new Prompt.Result(1));
            assertRejected(inputMethod, choices, "Miss");
        }
    }

    @Test
    public void testSimpleSRMultiplePhrasesHypothesis() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo, Mam"), //
                new Choice("My name is Bar, Mam"), //
                new Choice("My name is Foobar, Mam"));
        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            assertRejected(inputMethod, choices, "My name is");
            assertRejected(inputMethod, choices, "My name is Foo");
            assertRecognizedAsHypothesis(inputMethod, choices, "My name is Foobar", new Prompt.Result(2));
        }
    }

    private Choices simpleSRirregularPhrases() {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice(sorry), new Choice(ready), new Choice(haveIt), new Choice(ready2), new Choice(ready3));
        return choices;
    }

    @Test
    public void testSimpleSRirregularPhrases() throws InterruptedException {

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            Choices choices = simpleSRirregularPhrases();
            assertRecognized(inputMethod, choices);
        }
    }

    @Test
    public void testSimpleSRirregularPhrasesHypootheses() throws InterruptedException {

        try (SpeechRecognizer recognizers = SpeechRecognitionTestUtils.getRecognizer(TeaseLibSRSimple.class);
                SpeechRecognitionInputMethod inputMethod = new SpeechRecognitionInputMethod(recognizers)) {

            Choices choices = simpleSRirregularPhrases();
            assertRecognizedAsHypothesis(inputMethod, choices, "No Miss", new Prompt.Result(0));
            assertRecognizedAsHypothesis(inputMethod, choices, "Yes Miss", new Prompt.Result(1));
            assertRecognizedAsHypothesis(inputMethod, choices, "I have", new Prompt.Result(2));
            assertRecognizedAsHypothesis(inputMethod, choices, "Yes it's", new Prompt.Result(3));
            assertRecognizedAsHypothesis(inputMethod, choices, "It's ready", new Prompt.Result(4));

            Choices chat = as(choices, Intention.Chat);
            assertRejected(inputMethod, chat, "I'm Sorry", "Ready, Miss", "Ready");
        }
    }

}
