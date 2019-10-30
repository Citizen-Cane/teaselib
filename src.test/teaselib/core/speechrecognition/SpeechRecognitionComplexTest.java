package teaselib.core.speechrecognition;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.assertEqualsFlattened;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.assertRecognized;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.assertRejected;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.withoutPunctation;
import static teaselib.core.speechrecognition.srgs.Phrases.oneOf;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Ignore;
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

    private static final HashSet<Integer> CHOICES_0_1 = new HashSet<>(Arrays.asList(0, 1));
    private static final HashSet<Integer> CHOICES_0_3 = new HashSet<>(Arrays.asList(0, 3));
    private static final HashSet<Integer> CHOICES_1_2 = new HashSet<>(Arrays.asList(1, 2));
    private static final HashSet<Integer> CHOICES_2_3 = new HashSet<>(Arrays.asList(2, 3));
    private static final HashSet<Integer> CHOICES_0_1_2_3 = new HashSet<>(Arrays.asList(0, 1, 2, 3));

    @Test
    public void testRejected() throws InterruptedException {
        Choices choices = new Choices(new Choice("Foo", "Foo", "Foo"));
        assertRejected(choices, "Bar");
    }

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

        assertRejected(choices, "Yes Miss");
        assertRejected(choices, "Of course");
        assertRejected(choices, withoutPunctation("Miss"));
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

        // TODO Desired - remove joining empty phrase with neighboring phrase
        // assertEquals(Phrases.rule(0, 0, Phrases.oneOf(CHOICES_0_1, "I")), phrases.get(0));
        // assertEquals(Phrases.rule(0, 1, Phrases.oneOf(0, ""), Phrases.oneOf(1, "don't")), phrases.get(1));
        // assertEquals(Phrases.rule(0, 2, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(2));
        // assertEquals(3, phrases.size());

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(1));
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
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
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
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(3));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(4));

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

        assertEquals(Phrases.rule(0, 0, oneOf(new HashSet<>(asList(0, 1)), "Dear", "Please")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, oneOf(new HashSet<>(asList(0, 1)), "mistress may I")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, oneOf(0, "cum"), oneOf(1, "wank")), phrases.get(2));

        assertEquals(3, phrases.size());
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

    private static Choices phrasesWithMultipleCommonEndGroups() {
        String[] cum = { "May I cum, please", "May I cum, dear Mistress" };
        String[] wank = { "May I wank, please", "May I wank, dear Mistress" };
        return new Choices(new Choice("May I cum, please", "May I cum, please", cum),
                new Choice("May I wank, please", "May I wank, please", wank));
    }

    @Test
    @Ignore
    public void testSlicePhrasesWithMultipleCommonEndGroups() {
        Choices choices = phrasesWithMultipleCommonEndGroups();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, oneOf(new HashSet<>(asList(0, 1)), "May I")), phrases.get(0));
        // slicing cum/wank to choice groups doesn't work because slice() detects the remainder as all different
        // works perfect in testSlicePhrasesWithMultipleCommonStartGroups()
        // because there the last word is left after the common part has been matched
        // TODO Make slice() slice common groups instead of only true common part
        assertEquals(Phrases.rule(0, 1, oneOf(0, "cum"), oneOf(1, "wank")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, oneOf(new HashSet<>(asList(0, 1)), "please", "dear Mistress")), phrases.get(2));

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderPhrasesWithSeveralCommonEndGroups() throws InterruptedException {
        Choices choices = phrasesWithMultipleCommonEndGroups();

        assertRecognized(choices, withoutPunctation("May I cum, please"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("May I cum, dear mistress"), new Prompt.Result(0));

        assertRecognized(choices, withoutPunctation("May I wank, please"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("May I wank, dear mistress"), new Prompt.Result(1));

        assertRejected(choices, "May I cum");
        assertRejected(choices, "May I wank");

        assertRejected(choices, "please");
        assertRejected(choices, "Dear Mistress");
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
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderIdenticalPhrasesInDifferentChoices() throws InterruptedException {
        Choices choices = identicalPhrasesInDifferentChoices();

        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("No, of course not, Miss"), new Prompt.Result(1));
        assertRecognized(choices, "Yes Miss, of course, Miss", new Prompt.Result(0));
        assertRecognized(choices, "Yes Miss, of course not, Miss", new Prompt.Result(1));

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

    private static Choices distinctChociesWithPairwiseCommonParts() {
        return new Choices(new Choice("A at M attached"), new Choice("A at N attached"), new Choice("B at N attached"),
                new Choice("B at M attached"));
    }

    @Test
    public void testSliceDistinctChociesWithPairwiseCommonParts() {
        Choices choices = distinctChociesWithPairwiseCommonParts();
        Phrases phrases = Phrases.of(choices);

        assertEquals(4, phrases.size());
        assertEquals(Phrases.rule(0, 0, oneOf(CHOICES_0_1, "A"), oneOf(CHOICES_2_3, "B")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, oneOf(CHOICES_0_1_2_3, "at")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, oneOf(CHOICES_0_3, "M"), oneOf(CHOICES_1_2, "N")), phrases.get(2));
        assertEquals(Phrases.rule(0, 3, oneOf(CHOICES_0_1_2_3, "attached")), phrases.get(3));

        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderDistinctChociesWithPairwiseCommonParts() throws InterruptedException {
        Choices choices = distinctChociesWithPairwiseCommonParts();

        assertRecognized(choices, withoutPunctation("A at M attached"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("A at N attached"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("B at N attached"), new Prompt.Result(2));
        assertRecognized(choices, withoutPunctation("B at M attached"), new Prompt.Result(3));
    }

    private static Choices distinctChociesWithPairwiseCommonPartsShort() {
        return new Choices(new Choice("A M"), new Choice("A N"), new Choice("B N"), new Choice("B M"));
    }

    @Test
    public void testSliceDistinctChociesWithPairwiseCommonPartsShort() {
        Choices choices = distinctChociesWithPairwiseCommonPartsShort();
        Phrases phrases = Phrases.of(choices);

        // TODO Should be 2 but distinct parts are merged since the sequence-slicer splits alternating distinct-common
        // -> change the slicer to consider chunks that are partially common slices
        assertEquals(2, phrases.size());
        assertEquals(Phrases.rule(0, 0, oneOf(CHOICES_0_1, "A"), oneOf(CHOICES_2_3, "B")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, oneOf(CHOICES_0_3, "M"), oneOf(CHOICES_1_2, "N")), phrases.get(1));
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSRGSBuilderDistinctChociesWithPairwiseCommonPartsShort() throws InterruptedException {
        Choices choices = distinctChociesWithPairwiseCommonPartsShort();

        assertRecognized(choices, withoutPunctation("A M"), new Prompt.Result(0));
        assertRecognized(choices, withoutPunctation("A N"), new Prompt.Result(1));
        assertRecognized(choices, withoutPunctation("B N"), new Prompt.Result(2));
        assertRecognized(choices, withoutPunctation("B M"), new Prompt.Result(3));
    }

}
