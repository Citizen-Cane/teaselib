package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.*;
import static teaselib.core.speechrecognition.srgs.Phrases.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class PhrasesSmokeTest {

    private static final HashSet<Integer> CHOICES_0_1 = new HashSet<>(Arrays.asList(0, 1));

    private static Choices singleChoiceMultiplePhrasesAreDistinct() {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfOfStringsTwoOptionalParts() {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss of course", "of course Miss")), phrases.get(0));
        assertEquals(1, phrases.size());
    }

    // TODO Figure out how to turn "of course" into a common phrase
    @Test
    @Ignore
    public void testSliceMultipleChoiceSinglePhraseOfOfStringsTwoOptionalPartsOptimized() {
        Choices choices = singleChoiceMultiplePhrasesAreDistinct();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), 1);
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "Miss")), phrases.get(2));

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private static Choices optionalPhraseToDistiniguishMulitpleChoices() {
        return new Choices(new Choice("I have it"), new Choice("I don't have it"));
    }

    @Test
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoices() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(1));

        assertEquals(2, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    @Ignore
    // TODO Split into three rules and detect different uses of "I" in SRGS
    public void testSliceOptionalPhraseToDistiniguishMulitpleChoicesOptionalPartsOptimized() {
        Choices choices = optionalPhraseToDistiniguishMulitpleChoices();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(CHOICES_0_1, "I")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(0, ""), Phrases.oneOf(1, "don't")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(2));

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
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
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private static Choices multipleChoicesAlternativePhrases4() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultipleChoicesAlternativePhrases4() {
        Choices choices = multipleChoicesAlternativePhrases4();
        Phrases phrases = Phrases.of(choices);

        assertEquals(1, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    @Ignore
    // common phrase "of course" is not sliced to common rule
    // Yes Miss, of course
    // Yes , of course, Miss
    // Yes , of course
    // of course
    // No Miss, of course not
    // No , of course not, Miss
    // No , of course not
    // of course not
    // TODO make "of course" a separate group - detect smallest phrase contained in other phrases
    // - "of course" -> "of course Miss" -> "Yes of course Miss"
    public void testSliceMultipleChoicesAlternativePhrases4Optimized() {
        Choices choices = multipleChoicesAlternativePhrases4();
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "Miss"), Phrases.oneOf(1, "not Miss", "not")), phrases.get(2));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(2));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(1));

        assertEquals(5, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private static Choices multipleChoicesAlternativePhrases5() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss", "Yes, of course", "Of course, Miss",
                "Of course" };
        String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not, Miss",
                "Of course not" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceMultipleChoicesAlternativePhrases5() {
        Choices choices = multipleChoicesAlternativePhrases5();
        Phrases phrases = Phrases.of(choices);

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
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "Yes Miss", "Yes"), Phrases.oneOf(1, "No Miss", "No")),
                phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "", "Miss"), Phrases.oneOf(1, "not", "not Miss")),
                phrases.get(2));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(3));
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(4));

        assertEquals(5, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSliceMultipleGroupsDifferentPhraseOrdering() {
        String[] yes = { "I have it", "Yes, of course, Miss", "Yes Miss, of course" };
        String[] no = { "I don't have it", "No, of course not, Miss", "No Miss, of course not" };
        Choices choices = new Choices(new Choice("Yes", "Yes", yes), new Choice("No", "No", no));
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(1));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "Yes", "Yes Miss"), Phrases.oneOf(1, "No", "No Miss")),
                phrases.get(2));
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(3));
        assertEquals(Phrases.rule(1, 2, Phrases.oneOf(0, "Miss", ""), Phrases.oneOf(1, "not Miss", "not")),
                phrases.get(4));

        assertEquals(5, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    @Test
    public void testSliceMultipleGroupsDifferentPhraseOrderingGroupsMixed() {
        String[] yes = { "I have it", "Yes, of course, Miss", "Yes Miss, of course" };
        String[] no = { "No, of course not, Miss", "I don't have it", "No Miss, of course not" };
        Choices choices = new Choices(new Choice("Yes", "Yes", yes), new Choice("No", "No", no));
        Phrases phrases = Phrases.of(choices);

        assertEquals(Phrases.rule(0, 0, Phrases.oneOf(0, "I"), Phrases.oneOf(1, "I don't")), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "have it")), phrases.get(1));
        assertEquals(Phrases.rule(1, 0, Phrases.oneOf(0, "Yes", "Yes Miss"), Phrases.oneOf(1, "No", "No Miss")),
                phrases.get(2));
        assertEquals(Phrases.rule(1, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(3));
        assertEquals(Phrases.rule(1, 2, Phrases.oneOf(0, "Miss", ""), Phrases.oneOf(1, "not Miss", "not")),
                phrases.get(4));

        assertEquals(5, phrases.size());
        // Not exact because phrase groups have different positions in the phrases of each choice, but sufficient
        assertChoicesAndPhrasesMatch(choices, phrases);
    }

    @Test
    public void testSliceCommonMiddleEndWithTrailingEmptyChoiceString() {
        Choices choices = new Choices(new Choice("Yes Miss, I've spurted off"),
                new Choice("No Miss, I didn't spurt off"));

        Phrases phrases = Phrases.of(choices);

        assertEquals(4, phrases.size());
        assertEqualsFlattened(choices, phrases);
    }

    private static Choices simpleSRirregularPhrases() {
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
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4)), "Miss")),
                phrases.get(1));
        assertEquals(Phrases.rule(0, 2, Phrases.oneOf(0, "I'm sorry"), Phrases.oneOf(1, "I'm ready"),
                Phrases.oneOf(new HashSet<>(Arrays.asList(2, 3, 4)), "")), phrases.get(2));

        assertEquals(3, phrases.size());
        assertEqualsFlattened(choices, phrases);
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

    private static Choices identicalPhrasesInDifferentChoices2() {
        String[] yes = { "Yes, of course, Miss", "Yes Miss, of course" };
        String[] no = { "No, of course not, Miss", "Yes Miss, of course" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceIdenticalPhrasesInDifferentChoices2() {
        Choices choices = identicalPhrasesInDifferentChoices2();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        // Not exact because of duplicated phrases, but sufficient
        assertChoicesAndPhrasesMatch(choices, phrases);
    }

    private static Choices identicalPhrasesInDifferentChoices3() {
        String[] yes = { "Yes Miss, of course", "Yes, of course, Miss" };
        String[] no = { "No Miss, of course not", "Yes , of course, Miss" };
        return new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
    }

    @Test
    public void testSliceIdenticalPhrasesInDifferentChoices3() {
        Choices choices = identicalPhrasesInDifferentChoices3();
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        // Not exact because of duplicated phrases, but sufficient
        assertChoicesAndPhrasesMatch(choices, phrases);
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
    public void testSliceMYesMissIdid() {
        String[] yes = { "Yes Miss, I did", "Yes I did, Miss Rakhee", "Yes I did, Miss" };
        String[] no = { "No Miss, I didn't", "No I didn't, Miss", "No I didn't, Miss" };
        Choices choices = new Choices(new Choice("Yes #title, I did", "Yes Miss, I did", yes),
                new Choice("No #title, I didn't", "No Miss, I didn't", no));
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());
        // TODO flatten() still fails in some situations
        // assertChoicesAndPhrasesMatch(choices, phrases);
    }
}
