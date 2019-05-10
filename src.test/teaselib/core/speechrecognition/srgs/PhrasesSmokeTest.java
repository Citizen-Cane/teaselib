package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class PhrasesSmokeTest {

    private static Choices singleChoiceMultiplePhrasesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfOfStringsTwoOptionalParts() {
        Phrases phrases = Phrases.ofSliced(singleChoiceMultiplePhrasesAreDistinct());

        assertEquals(3, phrases.size());
    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoices() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.of(choices);

        assertEquals(2, phrases.size());
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
        Phrases phrases = Phrases.ofSliced(choices);

        assertEquals(3, phrases.size());

        // TODO the first rule has to be a common rule but isn't
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(Phrases.COMMON_RULE, "Dear", "Please")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "Mistress may I")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "cum"), Phrases.oneOf(1, "wank")), phrases.get(2));
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

        // TODO All optional phrases are reduced - but "of course" should be common
        // -> join only one common rule, or better none at all
        assertEquals(3, phrases.size());
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

        // TODO Unnecessary joining since choices are present and "Of course" just makes alternative phrases optional
        assertEquals(3, phrases.size());
    }
}
