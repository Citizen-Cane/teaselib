package teaselib.core.speechrecognition;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.assertEqualsFlattened;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.assertFlattenedMatchesPhrases;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.assertRecognized;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.assertRejected;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.withoutPunctation;
import static teaselib.core.speechrecognition.srgs.Phrases.oneOf;

import java.util.Arrays;

import org.junit.Test;

import teaselib.core.speechrecognition.srgs.Phrases;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.Prompt;

/**
 * @author Citizen-Cane
 *
 */
public class SpeechRecognitionComplexTest {

    private static Choices singleChoiceMultiplePhrasesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes));
    }

    @Test
    public void testSliceSingleChoiceMultiplePhrasesAreDistinct() {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(1, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderSingleChoiceMultiplePhrasesAreDistinct() throws InterruptedException {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Of course, Miss"), new Prompt.Result(0));

        assertRejected(choices, "Of course");
        assertRejected(choices, "Yes Miss Yes Miss");
        assertRejected(choices, "Of course of course");
        assertRejected(choices, withoutPunctation("Yes Miss, of course of course"));
        assertRejected(choices, withoutPunctation("Of course of course, Miss"));
    }

    private static Choices multipleChoicesAlternativePhrases() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultipleChoicesAlternativePhrases() {
        Choices choices = multipleChoicesAlternativePhrases();
        Phrases phrases = Phrases.of(choices);

        assertEquals(1, phrases.size());
        assertEquals(2, phrases.get(0).size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderMultipleChoicesAlternativePhrases() throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrases();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("of course"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("of course not"), new Prompt.Result(1, 1));

        assertRejected(choices, "Yes Miss");
        assertRejected(choices, "No Miss");

        assertRejected(choices, "No Miss of course");
        assertRejected(choices, "Of not");
    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoices() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "have it")), phrases.get(1));

        assertEquals(2, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderOptionalPhraseToDistiniguishMulitpleChoices() throws InterruptedException {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        assertRecognized(choices, "I have it", new Prompt.Result(0));
        assertRecognized(choices, "I don't have it", new Prompt.Result(1));
    }

    private static Choices multiplePhrasesOfMultipleChoicesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultiplePhrasesOfMultipleChoicesAreDistinct() {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));

        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinct() throws InterruptedException {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinct();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1));

        assertRejected(choices, "Yes Miss");
        assertRejected(choices, "No Miss");

        assertRejected(choices, "No Miss of course");
        assertRejected(choices, "Of not");

        assertRejected(choices, "Of course");
        assertRejected(choices, "Of course not");
    }

    private static Choices multipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course", "I have it" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not",
                "I don't have it" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct() {
        Choices choices = multipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderMultipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct()
            throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrasesWithOptionalPartsAreDistinct();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("of course"), new Prompt.Result(0, 0, 0));
        assertRecognized(choices, withoutPunctation("I have it"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("of course not"), new Prompt.Result(1, 1, 1));
        assertRecognized(choices, withoutPunctation("I don't have it"), new Prompt.Result(1));

        assertRejected(choices, "Yes Miss");
        assertRejected(choices, "No Miss");

        assertRejected(choices, "No Miss of course");
        assertRejected(choices, "Of not");
    }

    private static Choices multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "I have it" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "I don't have it" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts() {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(3));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "have it")), phrases.get(4));

        assertEquals(5, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
            throws InterruptedException {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0, 0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0, 0, 0));
        assertRecognized(choices, withoutPunctation("I have it"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("No Miss, of course not"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("No, of course not"), new Prompt.Result(1, 1, 1, 1));
        assertRecognized(choices, withoutPunctation("I don't have it"), new Prompt.Result(1));

        assertRejected(choices, "Yes Miss");
        assertRejected(choices, "No Miss");

        assertRejected(choices, "No Miss of course");
        assertRejected(choices, "Of not");

        assertRejected(choices, "Of course");
        assertRejected(choices, "Of course not");
    }

    private static Choices phrasesWithMultipleCommonStartGroups() {
        String[] cum = { "Dear mistress, may I cum", "Please mistress, may I cum" };
        String[] wank = { "Dear mistress, may I wank", "Please mistress, may I wank" };
        return new Choices(new Choice("Dear mistress, may I cum", "Dear mistress, may I cum", cum),
                new Choice("Dear mistress, may I wank", "Dear mistress, may I wank", wank));
    }

    @Test
    public void testSlicePhrasesWithMultipleCommonStartGroups() {
        Choices choices = phrasesWithMultipleCommonStartGroups();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());

        assertEquals(Phrases.rule(0, 0, oneOf(asList(0, 1), "Dear", "Please")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, oneOf(asList(0, 1), "mistress may I")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, oneOf(0, "cum"), oneOf(1, "wank")), phrases.get(2));

        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderPhrasesWithSeveralCommonStartGroups() throws InterruptedException {
        Choices choices = phrasesWithMultipleCommonStartGroups();

        assertRecognized(choices, withoutPunctation("Dear mistress, may I cum"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Please mistress, may I cum"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("Dear mistress, may I wank"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("Please mistress, may I wank"), new Prompt.Result(1));

        assertRejected(choices, "Please Mistress");
        assertRejected(choices, "Dear Mistress");

        assertRejected(choices, "May I cum");
        assertRejected(choices, "May I wank");

        assertRejected(choices, "Mistress, may I");
    }

    private static Choices identicalPhrasesInDifferentChoices() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss" };
        String[] no = { "Yes Miss, of course", "No, of course not, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceIdenticalPhrasesInDifferentChoices() {
        Choices choices = identicalPhrasesInDifferentChoices();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        assertFlattenedMatchesPhrases(choices, phrases);
    }

    // TODO All phrases are common - causes empty one-of element in main rule
    // - caused by "no" not recognized as choice 1, but maybe caused by the same rule in multiple choices
    // -> it's an edge case
    @Test
    public void testSRGSBuilderIdenticalPhrasesInDifferentChoices() throws InterruptedException {
        Choices choices = identicalPhrasesInDifferentChoices();

        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1));

        assertRejected(choices, "Yes Miss, of course");
    }

    private static Choices oneOfCommonAndChoicesMixed() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss" };
        String[] no = { "No Miss, of course", "No, of course not, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceOneOfCommonAndChoicesMixed() {
        Choices choices = oneOfCommonAndChoicesMixed();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderOneOfCommonAndChoicesMixed() throws InterruptedException {
        Choices choices = oneOfCommonAndChoicesMixed();

        assertRejected(choices, withoutPunctation("of course"));

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("No Miss, of course"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1));
    }

}
