package teaselib.core.speechrecognition;

import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;
import teaselib.core.ui.SpeechRecognitionInputMethod;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionTest {

    @Test
    public void testSRGSBuilderSinglePhrase() throws InterruptedException {
        Choices choices = new Choices(Arrays.asList(new Choice("Foo bar")));

        assertRecognized(choices, "Foo bar", new Prompt.Result(0));
        assertRejected(choices, "Foo");
        assertRejected(choices, "Bar");
    }

    @Test
    public void testSRGSBuilderCommonStart() throws InterruptedException {
        Choices choices = new Choices(new Choice("Please Miss, one more"), new Choice("Please Miss, one less"),
                new Choice("Please Miss, two more"));

        assertRecognized(choices, "Please Miss one more", new Prompt.Result(0));
        assertRecognized(choices, "Please Miss one less", new Prompt.Result(1));
        assertRecognized(choices, "Please Miss two more", new Prompt.Result(2));
        assertRejected(choices, "Please Miss three more May I");
        assertRejected(choices, "Please Miss one");
    }

    @Test
    public void testSRGSBuilderCommonEnd() throws InterruptedException {
        Choices choices = new Choices(new Choice("I've spurted my load, Dear Mistress"),
                new Choice("I didn't spurt off, Dear Mistress"));

        assertRecognized(choices, "I've spurted my load Dear Mistress", new Prompt.Result(0));
        assertRecognized(choices, "I didn't spurt off Dear Mistress", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Dear Mistress");
        assertRejected(choices, "I didn't spurt off Dear");
    }

    @Test
    public void testSRGSBuilderCommonMiddleEnd() throws InterruptedException {
        Choices choices = new Choices(new Choice("Yes Miss, I've spurted off"),
                new Choice("No Miss, I didn't spurt off"));

        assertRecognized(choices, "Yes Miss I've spurted off", new Prompt.Result(0, 0));
        assertRecognized(choices, "No Miss I didn't spurt off", new Prompt.Result(1, 1));
        assertRejected(choices, "Yes Miss I didn't spurt off");
        assertRejected(choices, "No Miss I've spurted off");
        assertRejected(choices, "Miss I've spurted off");
        assertRejected(choices, "Miss I didn't spurt off");
    }

    @Test
    public void testSRGSBuilderCommonStartEnd() throws InterruptedException {
        Choices choices = new Choices(new Choice("Dear Mistress, I've spurted my load, Miss"),
                new Choice("Dear Mistress I didn't spurt off, Miss"));

        assertRecognized(choices, "Dear Mistress I've spurted my load Miss", new Prompt.Result(0));
        assertRecognized(choices, "Dear Mistress I didn't spurt off Miss", new Prompt.Result(1));
        assertRejected(choices, "I didn't spurt my load Miss");
        assertRejected(choices, "Dear Mistress I didn't spurt off");
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

        assertRecognized(choices, sorry, new Prompt.Result(0));
        assertRecognized(choices, ready, new Prompt.Result(1));
        assertRecognized(choices, haveIt, new Prompt.Result(2));
        assertRecognized(choices, ready2, new Prompt.Result(3));
        assertRecognized(choices, ready3, new Prompt.Result(4));
    }

    @Test
    public void testUnicode() throws InterruptedException {
        String a = "Äh";
        String o = "Öh";
        String u = "Üh";

        Choices choices = new Choices(Arrays.asList(new Choice(a), new Choice(o), new Choice(u)));

        assertRecognized(choices, a, new Prompt.Result(0));
        assertRecognized(choices, o, new Prompt.Result(1));
        assertRecognized(choices, u, new Prompt.Result(2));
    }

    @Test
    public void testSRGSBuilderSimilarEndAmbiguity() throws InterruptedException {
        Choices choices = new Choices(new Choice("Yes I have"), new Choice("No I haven't"));

        List<Rule> result = new ArrayList<>();
        result.addAll(assertRecognized(choices, "Yes I have", new Prompt.Result(0)));
        result.addAll(assertRejected(choices, "Yes I haven't"));
        assertEquals(2, result.size());

        Rule distinct = SpeechRecognitionInputMethod.distinct(result).orElseThrow();
        assertEquals(result.get(0), distinct);

        assertRejected(choices, "Yes I haven't");
        assertRejected(choices, "No I have");
    }

    @Test
    public void testSRGSBuilderSimilarEndAmbiguity2() throws InterruptedException {
        Choices choices = new Choices(new Choice("Yes I have"), new Choice("No I haven't"));

        List<Rule> result = new ArrayList<>();
        result.addAll(assertRejected(choices, "Yes I haven't"));
        result.addAll(assertRecognized(choices, "Yes I have", new Prompt.Result(0)));
        assertEquals(2, result.size());

        Rule distinct = SpeechRecognitionInputMethod.distinct(result).orElseThrow();
        assertEquals(result.get(1), distinct);
    }

}
