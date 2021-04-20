package teaselib.core.speechrecognition.sapi;

import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.SpeechRecognitionInputMethod.*;
import static teaselib.core.speechrecognition.sapi.SpeechRecognitionTestUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import teaselib.core.speechrecognition.PreparedChoices;
import teaselib.core.speechrecognition.Rule;
import teaselib.core.speechrecognition.SpeechRecognitionInputMethod;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTest {

    @Test
    public void testSRGSBuilderSinglePhrase() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Foo bar"));
        assertRecognized(choices, "Foo bar", new Prompt.Result(0));

        Choices chat = as(choices, Intention.Chat);
        assertRejected(chat, "Foo");
        assertRejected(chat, "Bar");
    }

    @Test
    public void testSRGSBuilderCommonStart() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Please Miss, one more"), //
                new Choice("Please Miss, one less"), //
                new Choice("Please Miss, two more"));

        assertRecognized(choices, "Please Miss one more", new Prompt.Result(0));
        assertRecognized(choices, "Please Miss one less", new Prompt.Result(1));
        assertRecognized(choices, "Please Miss two more", new Prompt.Result(2));
        assertRejected(choices, "Please Miss three more May I");
        assertRejected(choices, "Please Miss one");
    }

    @Test
    public void testSRGSBuilderCommonEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("I've spurted my load, Dear Mistress"), new Choice("I didn't spurt off, Dear Mistress"));

        assertRecognized(choices, "I've spurted my load Dear Mistress", new Prompt.Result(0));
        assertRecognized(choices, "I didn't spurt off Dear Mistress", new Prompt.Result(1));
        assertRecognizedAsHypothesis(choices, "I didn't spurt off Dear", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Dear Mistress");
    }

    @Test
    public void testSRGSBuilderCommonMiddleEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes Miss, I've spurted off"), new Choice("No Miss, I didn't spurt off"));

        assertRecognized(choices, "Yes Miss I've spurted off", new Prompt.Result(0, 0));
        assertRecognized(choices, "No Miss I didn't spurt off", new Prompt.Result(1, 1));
        assertRejected(choices, "Yes Miss I didn't spurt off");
        assertRejected(choices, "No Miss I've spurted off");
        assertRejected(choices, "Miss I've spurted off");
        assertRejected(choices, "Miss I didn't spurt off");
    }

    @Test
    public void testSRGSBuilderCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Dear Mistress, I've spurted my load, Miss"),
                new Choice("Dear Mistress I didn't spurt off, Miss"));

        assertRecognized(choices, "Dear Mistress I've spurted my load Miss", new Prompt.Result(0));
        assertRecognized(choices, "Dear Mistress I didn't spurt off Miss", new Prompt.Result(1));
        assertRecognizedAsHypothesis(choices, "Dear Mistress I didn't spurt off", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Miss");
    }

    @Test
    public void tesSRGSMultiplePhrasesHypothesis() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("My name is Foo, Mam"), //
                new Choice("My name is Bar, Mam"), //
                new Choice("My name is Foobar, Mam"));
        assertRecognizedAsHypothesis(choices, "My name is Foo", new Prompt.Result(0));
        assertRecognizedAsHypothesis(choices, "My name is Bar", new Prompt.Result(1));
        assertRecognizedAsHypothesis(choices, "My name is Foobar", new Prompt.Result(2));
        assertRejected(choices, "My name is");
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

        assertRecognized(choices, withoutPunctation(sorry), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation(ready), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation(haveIt), new Prompt.Result(2));
        assertRecognized(choices, withoutPunctation(ready2), new Prompt.Result(3));
        assertRecognized(choices, withoutPunctation(ready3), new Prompt.Result(4));
    }

    @Test
    public void testUnicode() throws InterruptedException {
        String a = "Äh";
        String o = "Öh";
        String u = "Üh";
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice(a), new Choice(o), new Choice(u));

        try (SpeechRecognitionInputMethod inputMethod = getInputMethod(TeaseLibSRGS.Relaxed.class)) {
            assertRecognized(inputMethod, choices, a, new Prompt.Result(0));
            assertRecognized(inputMethod, choices, o, new Prompt.Result(1));
            assertRecognized(inputMethod, choices, u, new Prompt.Result(2));
        }
    }

    @Test
    public void testSRGSBuilderSimilarEndAmbiguity() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes I have"), //
                new Choice("No I haven't"));

        List<Rule> expected = new ArrayList<>();
        expected.addAll(assertRecognized(choices, "Yes I have", new Prompt.Result(0)));
        assertEquals(1, expected.size());

        List<Rule> rejected = new ArrayList<>();
        rejected.addAll(assertRejected(choices, "Yes I haven't"));
        assertEquals("Filtered by speech recognition implementation", 1, rejected.size());

        Rule distinct = bestSingleResult(expected, PreparedChoices.IdentityMapping).orElseThrow();
        assertEquals(expected.get(0), distinct);
        assertNotEquals(expected.get(0), rejected.get(0));

        assertRejected(choices, "Yes I haven't");
        assertRejected(choices, "No I have");
    }

    @Test
    public void testSRGSBuilderSimilarEndAmbiguity2() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes I have"), //
                new Choice("No I haven't"));

        List<Rule> rejected = new ArrayList<>();
        rejected.addAll(assertRejected(choices, "Yes I haven't"));
        if (rejected.size() > 1) {
            fail("Unstabel - multiple results: " + rejected);
        } else {
            assertEquals("Filtered by speech recognition implementation", 1, rejected.size());
        }

        List<Rule> expected = new ArrayList<>();
        expected.addAll(assertRecognized(choices, "Yes I have", new Prompt.Result(0)));
        assertEquals(1, expected.size());

        Rule distinct = bestSingleResult(expected, PreparedChoices.IdentityMapping).orElseThrow();
        assertEquals(expected.get(0), distinct);
        assertNotEquals(expected.get(0), rejected.get(0));
    }

    @Test
    public void testWeightedHypothesis() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes Mistress, I'ts locked up"), //
                new Choice("Sorry Mistress, not yet"));

        // Hypothesis is weighted by amount of complete phrase, resulting in a probability of 1.0 / 5 = 0.2
        assertRejected(choices, "Yes");
    }

    @Test
    public void testTrailingDominantNullRule() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Of course"), //
                new Choice("Of course not"));

        assertRecognized(choices, "Of course", new Prompt.Result(0));
        assertRecognized(choices, "Of course not", new Prompt.Result(1));

        Choices chat = as(choices, Intention.Chat);
        assertRejected(chat, "Of");
    }

    @Test
    public void testTrailingNonDominantNullRule() throws InterruptedException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes Of course"), //
                new Choice("No Of course not"));

        assertRecognized(choices, "Yes Of course", new Prompt.Result(0));
        assertRecognized(choices, "No Of course not", new Prompt.Result(1));
        assertRejected(choices, "Yes Of");
        assertRejected(choices, "No Of");

        Choices chat = as(choices, Intention.Chat);
        assertRecognizedAsHypothesis(chat, "Yes Of", new Prompt.Result(0));
        assertRecognizedAsHypothesis(chat, "No Of", new Prompt.Result(1));
    }

}
