package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Intention;

public class SRGSPhraseBuilderTest {

    @Test
    public void verifyThatPhraseStringSequenceToStringOutputsPlainWords() {
        Sequence<PhraseString> test = new Sequence<>(
                Arrays.asList(new PhraseString("Foo", 0), new PhraseString("bar", 0)), PhraseString.Traits);

        assertEquals("Foo bar", test.toString());
        assertFalse(test.toString().startsWith("["));
        assertFalse(test.toString().endsWith("]"));
    }

    @Test
    public void testCommonStart()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A, B"), new Choice("A, C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testCommonEnd()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A, C"), new Choice("B, C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testCommonMiddle()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A C, D"), new Choice("B C, E"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testCommonEdges()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B, D"), new Choice("A C, D"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testMultiplePhraseStartPositions()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B"), new Choice("B C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testSingleChoiceMultiplePhrasesAreDistinct()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("Yes #title, of course", "Yes Miss, of course", yes));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testMultiLevelCommon1()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B0 C0 D"), //
                new Choice("A B1 C0 D"), //
                new Choice("A B2 C2 D"), //
                new Choice("A B3 C2 D"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testMultiLevelCommon2()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, //
                new Choice("A B0 C0 D"), //
                new Choice("A B1 C0 D"), //
                new Choice("A B2 C2 D"), //
                new Choice("A B3 C2 D"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    private static final Choice Optional = new Choice("");

    @Test
    public void testSRGSBuilderMultipleChoiceResults() {
        String template = "A %0 %1, %2";
        Choice[] material = { new Choice("leather"), new Choice("rubber"), Optional };
        Choice[] dogToy = { new Choice("ball"), new Choice("bone"), new Choice("dildo") };
        Choice[] formOfAddress = { new Choice("#title", "Miss", "Miss", "Mistress", "dear Mistress") };
        Choice[][] args = { material, dogToy, formOfAddress };

        List<Sequences<PhraseString>> slices = new ArrayList<>();
        for (String word : PhraseString.words(template)) {
            if (word.startsWith("%")) {
                Choice[] arg = args[Integer.parseInt(word.substring(1))];
                PhraseStringSequences items = new PhraseStringSequences();
                for (int choiceIndex = 0; choiceIndex < arg.length; choiceIndex++) {
                    for (String phrase : arg[choiceIndex].phrases) {
                        items.add(new Sequence<>(new PhraseString(phrase, choiceIndex), PhraseString.Traits));
                    }
                }
                slices.add(new Sequences<>(items));
            } else {
                Sequences<PhraseString> sequences = new PhraseStringSequences(
                        new Sequence<>(new PhraseString(word, 0), PhraseString.Traits));
                slices.add(sequences);
            }
        }

        assertEquals(4, slices.size());
        // assertEquals(Phrases.rule(0, 0, "A"), phrases.get(0));
        // assertEquals(Phrases.rule(0, 1, "leather", "rubber", ""), phrases.get(1));
        // assertEquals(Phrases.rule(0, 2, "ball", "bone", "dildo"), phrases.get(2));
        // assertEquals(Phrases.rule(0, 3, Phrases.oneOf(0, "Miss", "Mistress", "dear Mistress")), phrases.get(3));
        //
        // assertTrue(phrases.get(0).get(0).contains("A"));
        // assertTrue(phrases.get(1).get(0).contains("leather"));
        // assertTrue(phrases.get(1).get(1).contains("rubber"));
        // assertTrue(phrases.get(1).get(2).contains(""));
        // assertTrue(phrases.get(2).get(0).contains("ball"));
        // assertTrue(phrases.get(2).get(1).contains("bone"));
        // assertTrue(phrases.get(2).get(2).contains("dildo"));
        //
        // assertTrue(phrases.get(3).get(0).contains("Miss"));
        // assertTrue(phrases.get(3).get(0).contains("Mistress"));
        // assertTrue(phrases.get(3).get(0).contains("dear Mistress"));

        // assertRecognized(phrases, "A rubber ball Miss", new Prompt.Result(0, 0));
        // assertRecognized(phrases, "A leather ball Miss", new Prompt.Result(1, 0));
        // assertRecognized(phrases, "A leather bone Miss", new Prompt.Result(1, 1));
        // assertRecognized(phrases, "A rubber bone Miss", new Prompt.Result(0, 1));
        // assertRecognized(phrases, "A dildo Miss", new Prompt.Result(2));
        // assertRecognized(phrases, "A rubber dildo Miss", new Prompt.Result(0, 2));
    }

}
