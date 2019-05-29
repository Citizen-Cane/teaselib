package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.srgs.Phrases.oneOf;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.core.speechrecognition.SpeechRecogntionTestUtils;
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

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss of course", "of course Miss")), phrases.get(0));
        assertEquals(1, phrases.size());
    }

    @Test
    @Ignore
    public void testSliceMultipleChoiceSinglePhraseOfOfStringsTwoOptionalPartsOptimized() {
        Phrases phrases = Phrases.ofSliced(singleChoiceMultiplePhrasesAreDistinct());

        // TODO Either "of course" must be common or must be different groups
        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss")), phrases.get(0));
        // TODO must be common part for both groups
        assertEquals(Phrases.rule(Phrases.COMMON_RULE, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")),
                phrases.get(phrases.size() - 1));
        // TODO Split into groups to avoid "of course" to be recognized
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(0, "Miss")), phrases.get(2));
        assertEquals(3, phrases.size());
    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoices() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.ofSliced(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "have it")), phrases.get(1));
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

        assertEquals(Phrases.rule(0, 0, oneOf(asList(0, 1), "Dear", "Please")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, oneOf(asList(0, 1), "mistress may I")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "cum"), Phrases.oneOf(1, "wank")), phrases.get(2));
        assertEquals(3, phrases.size());

        assertEqualsFlattened(choices, phrases);
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

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "Miss"), Phrases.oneOf(1, "not Miss", "not")), phrases.get(2));

        assertEquals(3, phrases.size());
        // TODO Flatten fails because optional phrase parts didn't make it into rule 2 -> recognition fails as well
        assertEqualsFlattened(choices, phrases);
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
        // Yes Miss, of course
        // Yes , of course, Miss
        // Yes , of course
        // of course
        // No Miss, of course not
        // No , of course not, Miss
        // No , of course not
        // of course not
        // TODO change data model to make this work
        // + OneOf multiple mixes too much -> use single OneOf for each phrase, build srgs over OneOf index in rule
        // TODO make SRGS handle multiples OneOf inventory keys per rule (currently it's ruleIndex-choice), add OneOf
        // index - that's group-like
        // -> add phrases in different OneOf elements, SRGS inventory references rules
        assertEquals(1, phrases.size());
        assertEqualsFlattened(choices, phrases);
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

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1), "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "Miss"), Phrases.oneOf(1, "not Miss", "not")), phrases.get(2));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(3));
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(Arrays.asList(0, 1), "have it")), phrases.get(4));

        assertEquals(5, phrases.size());
        // TODO Flatten fails because optional phrase parts didn't make it into rule 2 -> recognition fails as well
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSliceCommonMiddleEndWithTrailingEmptyChoiceString() {
        Choices choices = new Choices(new Choice("Yes Miss, I've spurted off"),
                new Choice("No Miss, I didn't spurt off"));

        Phrases phrases = Phrases.of(choices);

        assertEquals(4, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private Choices simpleSRirregularPhrases() {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        return new Choices(Arrays.asList(new Choice(sorry), new Choice(ready), new Choice(haveIt), new Choice(ready2),
                new Choice(ready3)));
    }

    @Test
    public void testSliceSimpleSRirregularPhrases() {
        Choices choices = simpleSRirregularPhrases();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "No"), Phrases.oneOf(1, "Yes"), Phrases.oneOf(2, "I have it"),
                Phrases.oneOf(3, "Yes it's ready"), Phrases.oneOf(4, "It's ready")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Arrays.asList(0, 1, 2, 3, 4), "Miss")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(Arrays.asList(0, 1), "I'm")), phrases.get(2));
        assertEquals(Phrases.rule(0, 3, Phrases.oneOf(0, "sorry"), Phrases.oneOf(1, "ready")), phrases.get(3));

        assertEquals(4, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private void assertEqualsFlattened(Choices choices, Phrases phrases) {
        Sequences<String> flattened = phrases.flatten();
        assertEquals(choices.size(), flattened.size());

        List<String> allChoices = firstOfEach(choices).stream().map(SpeechRecogntionTestUtils::withoutPunctation)
                .collect(toList());
        assertEquals(allChoices, flattened.toStrings());
    }

    private List<String> firstOfEach(Choices choices) {
        return choices.stream().map(p -> p.phrases.get(0)).collect(toList());
    }

}
