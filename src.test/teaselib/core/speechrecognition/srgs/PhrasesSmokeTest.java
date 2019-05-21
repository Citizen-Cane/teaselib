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

        // TODO Either "of course" must be common or must be different groups
        assertEquals(3, phrases.size());
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss")), phrases.get(0));
        // TODO must be common part for both groups
        assertEquals(Phrases.rule(Phrases.COMMON_RULE, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")),
                phrases.get(phrases.size() - 1));
        // TODO Split into groups to avoid "of course" to be recognized
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(0, "Miss")), phrases.get(2));

    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoices() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.ofSliced(choices);

        assertEquals(2, phrases.size());
        // TODO Words are joined, but rule 0,0 item 0 has to be choice 0 -> wrong join
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "have it")), phrases.get(1));
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
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(Phrases.COMMON_RULE, "Dear", "Please")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "mistress may I")), phrases.get(1));
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
        Phrases phrases = Phrases.ofSliced(choices);

        // TODO Decide what is better
        // assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes"), Phrases.oneOf(1, "No")), phrases.get(0));
        // assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "Miss", "")), phrases.get(1));
        // or (already correct)
        assertEquals(
                Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes", "Yes"), Phrases.oneOf(1, "No Miss", "No", "No")),
                phrases.get(0));

        // TODO "of course" joined with next slice - check empty slice
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));

        // TODO Join rule 2 & 3
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(Phrases.COMMON_RULE, "Miss"), Phrases.oneOf(1, "not")),
                phrases.get(2));

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
        Phrases phrases = Phrases.ofSliced(choices);

        // TODO "of course" not sliced to common or joined with empty slices
        assertEquals(3, phrases.size());
    }

    @Test
    public void testSliceMultipleGroups() {
        String[] yes = { //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "I have it" };
        String[] no = { //
                "No Miss, of course not", //
                "No, of course not, Miss", //
                "I don't have it" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
        Phrases phrases = Phrases.ofSliced(choices);

        assertEquals(4, phrases.size());

        // TODO assert rules
    }

}
