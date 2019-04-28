package teaselib.core.speechrecognition;

import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.SpeechRecogntionTestUtils.*;

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

        // Results in "of course" recognized
        // assertEquals(3, phrases.size());
        // assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "", "Yes Miss")), phrases.get(0));
        // assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        // assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "Miss", "")), phrases.get(2));

        // TODO Split groups into common and choice parts - "of course" is a common item

        assertEquals(4, phrases.size());
        assertEquals(Phrases.rule(0, 0, "Yes Miss"), phrases.get(0));
        assertEquals(Phrases.rule(1, 0, "Of course"), phrases.get(1));
        assertEquals(Phrases.rule(1, 1, "Of course"), phrases.get(2));
        assertEquals(Phrases.rule(1, 1, "Miss"), phrases.get(3));
    }

    @Test
    public void testSRGSBuilderSingleChoiceMultiplePhrasesAreDistinct() throws InterruptedException {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Of course, Miss"), new Prompt.Result(0, 0));
        assertRejected(choices, "Of course");
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

        assertEquals(3, phrases.size());

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes", ""), Phrases.oneOf(1, "No Miss", "No", "")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));
    }

    @Test
    public void testSRGSBuilderMultipleChoicesAlternativePhrases() throws InterruptedException {
        Choices choices = multipleChoicesAlternativePhrases();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course, Miss"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("Yes, of course"), new Prompt.Result(0, 0));
        assertRecognized(choices, withoutPunctation("of course"), new Prompt.Result(0, 0));

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

        assertEquals(3, phrases.size());
        // TODO Empty One-Of element of choice 0 fails in SRGSBuilder
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(Phrases.COMMON_RULE, "I")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(0, ""), Phrases.oneOf(1, "don't")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(Phrases.COMMON_RULE, "have it")), phrases.get(2));
    }

    @Test
    public void testSRGSBuilderOptionalPhraseToDistiniguishMulitpleChoices() throws InterruptedException {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        assertRecognized(choices, "I have it", new Prompt.Result(0));
        assertRecognized(choices, "I don't have it", new Prompt.Result(1));
    }

    @Test
    public void testSliceMultiplePhrasesOfMultipleChoicesAreDistinct() {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));
    }

    private static Choices multiplePhrasesOfMultipleChoicesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
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

        assertEquals(8, phrases.size());
        // TODO assert structure
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

        assertEquals(10, phrases.size());
        // TODO assert structure
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(0, "Miss", "")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "of course")), phrases.get(2));
    }

    @Test
    public void testSRGSBuilderMultiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts()
            throws InterruptedException {
        Choices choices = multiplePhrasesOfMultipleChoicesAreDistinctWithoutOptionalParts();

        assertRecognized(choices, withoutPunctation("Yes Miss, of course"), new Prompt.Result(0, 0, 0, 0));
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

}
