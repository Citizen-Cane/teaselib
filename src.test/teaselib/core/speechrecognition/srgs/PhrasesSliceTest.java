package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static teaselib.core.speechrecognition.srgs.PhraseString.Traits;
import static teaselib.core.speechrecognition.srgs.PhraseStringSequences.prettyPrint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.CodeDuration;

public class PhrasesSliceTest {
    private static final Logger logger = LoggerFactory.getLogger(PhrasesSliceTest.class);

    static PhraseStringSymbols choices(String... choices) {
        return new PhraseStringSymbols(Arrays.asList(choices));
    }

    static Sequence<PhraseString> result(String string1, Integer... choices) {
        return result(Collections.singletonList(string1), choices);
    }

    static Sequence<PhraseString> result(String string1, String string2, Integer... choices) {
        return result(Arrays.asList(string1, string2), choices);
    }

    static Sequence<PhraseString> result(String string1, String string2, String string3, Integer... choices) {
        return result(Arrays.asList(string1, string2, string3), choices);
    }

    static Sequence<PhraseString> result(String string1, String string2, String string3, String string4,
            Integer... choices) {
        return result(Arrays.asList(string1, string2, string3, string4), choices);
    }

    static Sequence<PhraseString> result(String string1, String string2, String string3, String string4, String string5,
            Integer... choices) {
        return result(Arrays.asList(string1, string2, string3, string4, string5), choices);
    }

    static Sequence<PhraseString> result(List<String> strings, Integer... choices) {
        Set<Integer> indices = Stream.of(choices).collect(toSet());
        return new Sequence<>(strings.stream().map(phrase -> new PhraseString(phrase, indices)).collect(toList()),
                Traits);
    }

    @SafeVarargs
    private static PhraseStringSequences results(Sequence<PhraseString>... results) {
        return new PhraseStringSequences(results);
    }

    final List<List<Sequences<PhraseString>>> candidates = new ArrayList<>();

    public static SlicedPhrases<PhraseString> slice(PhraseStringSymbols choices) {
        return slice(choices.joinDuplicates().toPhraseStringSequences());
    }

    public static SlicedPhrases<PhraseString> slice(PhraseStringSequences choices) {
        List<SlicedPhrases<PhraseString>> candidates = new ArrayList<>();
        SlicedPhrases<PhraseString> optimal = CodeDuration.executionTimeMillis(logger, "Slicing duration = {}ms",
                () -> {
                    PhraseStringSequences phrases = new PhraseStringSequences(choices);
                    return slice(phrases, candidates, PhraseStringSequences::prettyPrint);
                });

        assertSliced(choices, optimal);

        // TODO symbol count differs after joining in pre-processing
        // assertEquals("Distinct symbol count", Collections.singletonList((int) optimal.distinctSymbolsCount()),
        // candidates.stream().map(e -> e.rating.symbols.size()).distinct().collect(toList()));

        for (SlicedPhrases<PhraseString> candidate : candidates) {
            assertEquals(candidate.toString() + "\t duplicates", candidate.rating.duplicatedSymbols,
                    candidate.duplicatedSymbolsCount());

            assertEquals(candidate.toString() + "\t max commonness", candidate.rating.maxCommonness,
                    candidate.maxCommonness());

            assertEquals(candidate.toString() + "\t max commonness", candidate.rating.maxCommonness,
                    candidate.maxCommonness());
        }

        return optimal;
    }

    private static <T> SlicedPhrases<T> slice(Sequences<T> phrases, List<SlicedPhrases<T>> results,
            Function<Sequences<T>, String> toString) {
        SlicedPhrases.slice(results, phrases, toString);
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        results.removeIf(candidate -> candidate.rating.isInvalidated());
        results.stream().forEach(candidates::add);
        return SlicedPhrases.reduceNullRules(candidates.getResult());
    }

    static void assertSliced(PhraseStringSequences choices, SlicedPhrases<PhraseString> sliced) {
        for (int i = 0; i < choices.size(); i++) {
            Sequence<PhraseString> expected = choices.get(i);
            Sequence<PhraseString> actual = new Sequence<>(choices.traits);
            for (Sequences<PhraseString> sequences : sliced) {
                for (Sequence<PhraseString> fragment : sequences) {
                    if (fragment.get(0).indices.contains(i)) {
                        actual.addAll(fragment);
                    }
                }
            }
            assertEquals(sliced.toString() + "Phrase " + i, expected.toString().toLowerCase(),
                    actual.toString().toLowerCase());
        }
    }

    private static void assertSequence(Sequence<PhraseString> expected, SlicedPhrases<PhraseString> actual, int index) {
        assertSequence(new PhraseStringSequences(expected), actual, index);
    }

    private static void assertSequence(PhraseStringSequences expected, SlicedPhrases<PhraseString> actual, int index) {
        if (!Objects.equals(expected, actual.get(index))) {
            fail("Element " + index + ": expected <" + prettyPrint(expected) + "> but was <"
                    + prettyPrint(actual.get(index)) + ">");
        }
    }

    @Test
    public void testPhraseIntersect() {
        assertTrue(PhraseString.intersect(new HashSet<>(Arrays.asList(0, 1)), new HashSet<>(Arrays.asList(0, 1))));
        assertTrue(PhraseString.intersect(new HashSet<>(Arrays.asList(0, 2)), new HashSet<>(Arrays.asList(0, 1))));
        assertFalse(PhraseString.intersect(new HashSet<>(Arrays.asList(3, 2)), new HashSet<>(Arrays.asList(0, 1))));
        assertFalse(PhraseString.intersect(new HashSet<>(Arrays.asList()), new HashSet<>(Arrays.asList(0, 1))));
    }

    @Test
    public void testPhraseStringEquals() {
        assertEquals(new PhraseString("N", 5), new PhraseString("N", 5));
        assertEquals(new PhraseString("N", 5), new PhraseString("N", Integer.valueOf(5)));
        assertEquals(new PhraseString("N", 5), new PhraseString("N", Collections.singleton(5)));
        assertEquals(new PhraseString("N", 5), new PhraseString("n", 5));
    }

    @Test
    public void testCompletion() {
        var choices = choices( //
                "A B C D", //
                "B C D", //
                "C D", //
                "D", //
                "D E");
        SlicedPhrases<PhraseString> sliced = SlicedPhrases.of(choices);

        assertEquals(4, sliced.complete(new PhraseString("A", 0)).size());
        assertEquals(3, sliced.complete(new PhraseString("B", 1)).size());
        assertEquals(2, sliced.complete(new PhraseString("Foo", 2)).size());
        assertEquals(1, sliced.complete(new PhraseString("baz", 3)).size());
        assertEquals(2, sliced.complete(new PhraseString("Bar", 4)).size());
        assertEquals(2, sliced.complete(new PhraseString("Foo", 3, 4)).size());
    }

    @Test
    public void testProductionSlice() {
        var choices = choices( //
                "A B C D", //
                "B C D", //
                "C D", //
                "D");
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(choices);

        assertEquals(4, optimal.size());
        assertSequence(results(result("A", 0)), optimal, 0);
        assertSequence(results(result("B", 0, 1)), optimal, 1);
        assertSequence(results(result("C", 0, 1, 2)), optimal, 2);
        assertSequence(results(result("D", 0, 1, 2, 3)), optimal, 3);
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testProductionSlice2() {
        var choices = choices( //
                "A", //
                "A B", //
                "A B C", //
                "A B C D");
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(choices);

        assertEquals(4, optimal.size());
        assertSequence(results(result("A", 0, 1, 2, 3)), optimal, 0);
        assertSequence(results(result("B", 1, 2, 3)), optimal, 1);
        assertSequence(results(result("C", 2, 3)), optimal, 2);
        assertSequence(results(result("D", 3)), optimal, 3);
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testProducetionSliceNullRuleElimination() {
        var choices = choices( //
                "A B0 C0 D", //
                "A B1 C0 D", //
                "A B2 C2 D");
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(choices);

        assertSequence(results(result("A", 0, 1, 2)), optimal, 0);
        assertSequence(results(result("b0", 0), result("b1", 1), result("b2", 2)), optimal, 1);
        assertSequence(results(result("c0", 0, 1), result("c2", 2)), optimal, 2);
        assertSequence(results(result("d", 0, 1, 2)), optimal, 3);
        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testProductionSlice2Reverse() {
        var choices = choices( //
                "A B C D", //
                "A B C", //
                "A B", //
                "A"); //
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(choices);

        assertEquals(4, optimal.size());
        assertSequence(results(result("A", 0, 1, 2, 3)), optimal, 0);
        assertSequence(results(result("B", 0, 1, 2)), optimal, 1);
        assertSequence(results(result("C", 0, 1)), optimal, 2);
        assertSequence(results(result("D", 0)), optimal, 3);
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testCommonStart() {
        var choices = choices( //
                "A, B", //
                "A, C");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("A", 0, 1)), optimal, 0);
        assertSequence(results(result("B", 0), result("C", 1)), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testCommonEnd() {
        var choices = choices( //
                "A, C", //
                "B, C");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("A", 0), result("B", 1)), optimal, 0);
        assertSequence(results(result("C", 0, 1)), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testYes3() {
        var choices = choices( //
                "yes Miss, of course", //
                "Yes, of Course, Miss", //
                "Of course");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("Yes", 0, 1)), optimal, 0);
        assertSequence(results(result("Miss", 0)), optimal, 1);
        assertSequence(results(result("of course", 0, 1, 2)), optimal, 2);
        assertSequence(results(result("Miss", 1)), optimal, 3);
        assertEquals(4, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testYes3b() {
        var choices = choices( //
                "Miss, of course", //
                "Yes, of Course, Miss", //
                "Of course");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("Miss", 0), result("Yes", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1, 2)), optimal, 1);
        assertSequence(results(result("Miss", 1)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testThatCommonSlicesConsiderSubsequentCommonSlices() {
        var choices = choices( //
                "yes Miss, of course", //
                "Yes, Of Course, Miss", //
                "Yes, of course", //
                "No Miss, of course not", //
                "No, of Course not, Miss", //
                "No, Of course not");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // TODO correctly symbolized, but sliced to slice 2 = "miss of course"=[0, 3] "of course"=[1, 2, 4, 5]
        // assertSequence(results(result("No", 3, 4, 5), result("Yes", 0, 1, 2)), optimal, 0);
        // assertSequence(results(result("Miss", 0, 3)), optimal, 1);
        // assertSequence(results(result("of", "course", 0, 1, 2, 3, 4, 5)), optimal, 2);
        // assertSequence(results(result("not", 3, 4, 5)), optimal, 3);
        // assertSequence(results(result("Miss", 1, 4)), optimal, 4);
        // assertEquals(5, optimal.size());
        // assertEquals(1, optimal.rating.duplicatedSymbols);
        // assertEquals(6, optimal.rating.maxCommonness);

        assertSequence(results(result("no", 3, 4, 5), result("yes", 0, 1, 2)), optimal, 0);
        assertSequence(results(result("miss", "of course", 0, 3), result("of course", 1, 2, 4, 5)), optimal, 1);
        assertSequence(results(result("not", 3, 4, 5)), optimal, 2);
        assertSequence(results(result("miss", 1, 4)), optimal, 3);
        // TODO should be 5 and max commonness =0 6 but shorter sliced phrases are rated higher
        assertEquals(4, optimal.size());
        assertEquals(2, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testThatCommonSlicesConsiderSubsequentCommonSlices_LowerCase() {
        var choices = choices( //
                "yes miss of course", //
                "yes of course miss", //
                "yes, of course", //
                "no miss, of course not", //
                "no of course not miss", //
                "no of course not");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // TODO correctly symbolized, but sliced to slice 2 = "miss of course"=[0, 3] "of course"=[1, 2, 4, 5]
        // assertSequence(results(result("no", 3, 4, 5), result("yes", 0, 1, 2)), optimal, 0);
        // assertSequence(results(result("miss", 0, 3)), optimal, 1);
        // assertSequence(results(result("of", "course", 0, 1, 2, 3, 4, 5)), optimal, 2);
        // assertSequence(results(result("not", 3, 4, 5)), optimal, 3);
        // assertSequence(results(result("miss", 1, 4)), optimal, 4);
        // // TODO should be 5 and max commonness =0 6 but shorter sliced phrases are rated higher
        // assertEquals(5, optimal.size());
        // assertEquals(1, optimal.rating.duplicatedSymbols);
        // assertEquals(6, optimal.rating.maxCommonness);

        assertSequence(results(result("no", 3, 4, 5), result("yes", 0, 1, 2)), optimal, 0);
        assertSequence(results(result("miss", "of course", 0, 3), result("of course", 1, 2, 4, 5)), optimal, 1);
        assertSequence(results(result("not", 3, 4, 5)), optimal, 2);
        assertSequence(results(result("miss", 1, 4)), optimal, 3);
        // TODO should be 5 and max commonness =0 6 but shorter sliced phrases are rated higher
        assertEquals(4, optimal.size());
        assertEquals(2, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testThatSliceCommonPrefersLargerCommonChunks() {
        var choices = choices( //
                "Miss, of course", //
                "of Course, Miss", //
                "of course", //
                "Miss, of course, Miss").joinDuplicates();

        assertEquals(choice("Miss", "of course"), choices.get(0));
        assertEquals(choice("of course", "Miss"), choices.get(1));
        assertEquals(choice("of course"), choices.get(2));
        assertEquals(choice("Miss", "of course", "Miss"), choices.get(3));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertSequence(results(result("Miss", 0, 3)), optimal, 0);
        assertSequence(results(result("of course", 0, 1, 2, 3)), optimal, 1);
        assertSequence(results(result("Miss", 1, 3)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance1() {
        var choices = choices( //
                "yes Miss, of course", //
                "No, of Course not, Miss");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result("yes", "Miss", 0), result("No", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Miss", 1)), optimal, 2);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance1Reverse() {
        var choices = choices( //
                "No, of Course not, Miss", //
                "yes Miss, of course").joinDuplicates();

        assertEquals(choice("No", "of Course", "not", "Miss"), choices.get(0));
        assertEquals(choice("yes", "Miss", "of Course"), choices.get(1));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result("No", 0), result("yes", "Miss", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Miss", 0)), optimal, 2);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance2() {
        var choices = choices( //
                "yes Mistress Lee, of course", //
                "No, of Course not, Mistress");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("yes", "Mistress", "Lee", 0), result("No", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Mistress", 1)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance2Reverse() {
        var choices = choices( //
                "No, of Course not, Mistress", //
                "yes Mistress Lee, of course");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("No", 0), result("yes", "Mistress", "Lee", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Mistress", 0)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance3() {
        var choices = choices( //
                "yes Mistress Lee La, of course", //
                "No, of Course not, Mistress");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("yes", "Mistress", "Lee La", 0), result("No", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Mistress", 1)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance3Reverse() {
        var choices = choices( //
                "No, of Course not, Mistress", //
                "yes Mistress Lee La, of course");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("No", 0), result("yes", "Mistress", "Lee La", 1)), optimal, 0);
        assertSequence(results(result("of course", 0, 1)), optimal, 1);
        assertSequence(results(result("not", "Mistress", 0)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesMixedCase() {
        var choices = choices( //
                "No Miss, I'm sorry", //
                "Yes Miss, I'm ready", //
                "I have it, Miss", //
                "Yes,it's ready, Miss", //
                "It's ready, Miss").joinDuplicates();

        assertEquals(choice("No", "Miss", "I'm", "sorry"), choices.get(0));
        assertEquals(choice("Yes", "Miss", "I'm", "ready"), choices.get(1));
        assertEquals(choice("I have it", "Miss"), choices.get(2));
        assertEquals(choice("Yes", "it's", "ready", "Miss"), choices.get(3));
        assertEquals(choice("it's", "ready", "Miss"), choices.get(4));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        // "I have it" is eventually sliced and moved, but pushes common "Miss" to match at the end
        assertSequence(results(result("Yes", 1, 3), result("No", 0), result("I", 2)), optimal, 0);
        assertSequence(results(result("it's", "ready", 3, 4), result("have", "it", 2)), optimal, 1);
        assertSequence(results(result("Miss", 0, 1, 2, 3, 4)), optimal, 2);
        assertSequence(results(result("I'm", 0, 1)), optimal, 3);
        assertSequence(results(result("sorry", 0), result("ready", 1)), optimal, 4);
    }

    StringSequence choice(String... symbols) {
        return new StringSequence(Arrays.asList(symbols));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesSingleDisjunctSymbol() {
        var choices = choices( //
                "No Miss, I'm sorry", //
                "Yes Miss, I'm ready", //
                "Okay, Miss", //
                "Yes,it's ready, Miss", //
                "It's ready, Miss").joinDuplicates();

        assertEquals(choice("No", "Miss", "I'm", "sorry"), choices.get(0));
        assertEquals(choice("Yes", "Miss", "I'm", "ready"), choices.get(1));
        assertEquals(choice("Okay", "Miss"), choices.get(2));
        assertEquals(choice("Yes", "it's", "ready", "Miss"), choices.get(3));
        assertEquals(choice("it's", "ready", "Miss"), choices.get(4));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        assertSequence(results(result("Yes", 1, 3), result("No", 0), result("Okay", 2)), optimal, 0);
        assertSequence(results(result("It's", "ready", 3, 4)), optimal, 1);
        assertSequence(results(result("Miss", 0, 1, 2, 3, 4)), optimal, 2);
        assertSequence(results(result("I'm", 0, 1)), optimal, 3);
        assertSequence(results(result("sorry", 0), result("ready", 1)), optimal, 4);
    }

    @Test
    public void testSliceMultipleCommon1ReduceNullRulesProduction() {
        var choices = choices( //
                "A B0 C0 D", //
                "A B1 C0 D", //
                "A B2 C2 D").joinDuplicates();

        assertEquals(choice("A", "B0", "C0", "D"), choices.get(0));
        assertEquals(choice("A", "B1", "C0", "D"), choices.get(1));
        assertEquals(choice("A", "B2 C2", "D"), choices.get(2));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertSequence(results(result("A", 0, 1, 2)), optimal, 0);
        assertSequence(results(result("b0", 0), result("b1", 1), result("b2", 2)), optimal, 1);
        assertSequence(results(result("c0", 0, 1), result("c2", 2)), optimal, 2);
        assertSequence(results(result("d", 0, 1, 2)), optimal, 3);
        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon2() {
        var choices = choices( //
                "A B0 C0 D", //
                "A B1 C0 D", //
                "A B2 C2 D", //
                "A B3 C2 D");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1, 2, 3), optimal, 0);
        assertSequence(results(result("b0", 0), result("b1", 1), result("b2", 2), result("b3", 3)), optimal, 1);
        assertSequence(results(result("c0", 0, 1), result("c2", 2, 3)), optimal, 2);
        assertSequence(results(result("d", 0, 1, 2, 3)), optimal, 3);
        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctationMarksAreIgnored() {
        PhraseStringSymbols choices = choices( //
                "A B C", //
                "A,B C.", //
                "a B,C.");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("A B C", 0, 1, 2)), optimal, 0);
        assertEquals(1, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon3() {
        var choices = choices( //
                "A B C, D E F G", //
                "D E F H, B C", //
                "D I J, B C", //
                "A, D I K L, B C", //
                "D I F M, B C", //
                "N B C, O");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // K L symbols are joined to a single symbol
        assertSequence(results(result("N", 5), result("A", 0, 3)), optimal, 0);
        assertSequence(result("B C", 0, 5), optimal, 1);
        assertSequence(results(result("O", 5), result("D", 0, 1, 2, 3, 4)), optimal, 2);
        assertSequence(results(result("E", "F", 0, 1), result("I", 2, 3, 4)), optimal, 3);
        assertSequence(results(result("G", 0), result("H", 1), result("J", 2), //
                result("K L", 3), result("F", "M", 4)), optimal, 4);
        assertSequence(results(result("B C", 1, 2, 3, 4)), optimal, 5);
        assertEquals(6, optimal.size());
        assertEquals(2, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);
    }

    public void testSliceMultipleCommon3Performance() {
        long start = System.currentTimeMillis();
        int n = 5;
        for (int i = 0; i < n; i++) {
            long now = System.currentTimeMillis();

            testSliceMultipleCommon3();
            testSliceMultipleCommon4();
            testSliceMultipleCommon4Hyphenation();
            testStackOverflow();
            testLongComputeTimeAndOutOfMemory();

            long finish = System.currentTimeMillis();
            logger.info("result = {}ms", finish - now);
        }
        long end = System.currentTimeMillis();
        logger.info("---------------------------------");
        logger.info("Overall = {}ms", end - start);
        logger.info("average = {}ms", (double) (end - start) / n);
    }

    @Test
    public void testSliceSplitVeryShortCommonStart() {
        var choices = choices( //
                "A B", //
                "A");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1), optimal, 0);
        assertSequence(result("B", 0), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceSplitVeryShortCommonEnd() {
        var choices = choices( //
                "A B", //
                "B");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0), optimal, 0);
        assertSequence(result("B", 0, 1), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceSplitShort() {
        var choices = choices( //
                "A B", //
                "A B", //
                "B", //
                "B");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1), optimal, 0);
        assertSequence(result("B", 0, 1, 2, 3), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceSplit() {
        var choices = choices( //
                "A B C D", //
                "A B C E", //
                "B C F", //
                "B C G").joinDuplicates();

        assertEquals(choice("A", "B C", "D"), choices.get(0));
        assertEquals(choice("A", "B C", "E"), choices.get(1));
        assertEquals(choice("B C", "F"), choices.get(2));
        assertEquals(choice("B C", "G"), choices.get(3));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertSequence(result("A", 0, 1), optimal, 0);
        // TODO B C should be joined but probably ignored because it is a first symbol
        assertSequence(result("B C", 0, 1, 2, 3), optimal, 1);
        assertSequence(results(result("D", 0), result("E", 1), result("F", 2), result("G", 3)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon4() {
        var choices = choices( //
                "A B C D E?", //
                "A B C F G H?", //
                "B C II F J K", //
                "B C L M N O P", //
                "B C L M Q RT", //
                "B C L M Q ST", //
                "B C II F J UV", //
                "B C W F N X Y U", //
                "A B C X Y U Z", //
                "B C 1 2 34 5", //
                "A B C F 6 7");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1, 8, 10), optimal, 0);
        assertSequence(result("B C", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), optimal, 1);
        assertSequence(
                results(result("D", 0), result("W", 7), result("1", 9), result("II", 2, 6), result("L M", 3, 4, 5)),
                optimal, 2);
        assertSequence(results(result("F", 1, 2, 6, 7, 10), result("Q", 4, 5), result("E", 0), result("2", 9)), optimal,
                3);
        assertSequence(results(result("G", 1), result("RT", 4), result("ST", 5), result("6", 10), result("J", 2, 6),
                result("N", 3, 7), result("34", 9)), optimal, 4);
        assertSequence(results(result("K", 2), result("O", 3), result("UV", 6), result("X Y U", 7, 8), result("H", 1),
                result("7", 10), result("5", 9)), optimal, 5);
        assertSequence(results(result("Z", 8), result("P", 3)), optimal, 6);

        assertEquals(7, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(11, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon4Hyphenation() {
        var choices = choices( //
                "A B C D E?", //
                "A B C F G H?", //
                "B C II F J K", //
                "B C L M N O P", //
                "B C L M Q R-T", //
                "B C L M Q S-T", //
                "B C II F J U-V", //
                "B C W F N X Y U", //
                "A B C X Y U Z", //
                "B C 1 2 3-4 5", //
                "A B C F 6 7");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1, 8, 10), optimal, 0);
        assertSequence(result("B C", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), optimal, 1);
        assertSequence(
                results(result("D", 0), result("W", 7), result("1", 9), result("II", 2, 6), result("L M", 3, 4, 5)),
                optimal, 2);
        assertSequence(results(result("F", 1, 2, 6, 7, 10), result("Q", 4, 5), result("E", 0), result("2", 9)), optimal,
                3);
        assertSequence(results(result("G", 1), result("R", 4), result("S", 5), result("6", 10), result("J", 2, 6),
                result("N", 3, 7), result("3", 9)), optimal, 4);
        assertSequence(results(result("T", 4, 5), result("K", 2), result("O", 3), result("U", 6), result("X Y U", 7, 8),
                result("H", 1), result("7", 10), result("4", 9)), optimal, 5);
        assertSequence(results(result("Z", 8), result("P", 3), result("V", 6), result("5", 9)), optimal, 6);

        assertEquals(7, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(11, optimal.rating.maxCommonness);
    }

    @Test
    public void testUnsliceableSliceOrderInvariance() {
        var choices = choices( //
                "A B C D E", //
                "E A B C D", //
                "D E A B C", //
                "C D E A B", //
                "B C D E A");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(9, optimal.size());
        assertEquals(4, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        assertSequence(results(result("A", 0)), optimal, 0);
        assertSequence(results(result("B", 0, 4)), optimal, 1);
        assertSequence(results(result("C", 0, 3, 4)), optimal, 2);
        assertSequence(results(result("D", 0, 2, 3, 4)), optimal, 3);
        assertSequence(results(result("E", 0, 1, 2, 3, 4)), optimal, 4);
        assertSequence(results(result("A", 1, 2, 3, 4)), optimal, 5);
        assertSequence(results(result("B", 1, 2, 3)), optimal, 6);
        assertSequence(results(result("C", 1, 2)), optimal, 7);
        assertSequence(results(result("D", 1)), optimal, 8);
    }

    @Test
    public void testUnsliceableSliceOrderInvarianceReverse() {
        var choices = choices( //
                "B C D E A", //
                "C D E A B", //
                "D E A B C", //
                "E A B C D", //
                "A B C D E");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(9, optimal.size());
        assertEquals(4, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        assertSequence(results(result("A", 4)), optimal, 0);
        assertSequence(results(result("B", 0, 4)), optimal, 1);
        assertSequence(results(result("C", 0, 1, 4)), optimal, 2);
        assertSequence(results(result("D", 0, 1, 2, 4)), optimal, 3);
        assertSequence(results(result("E", 0, 1, 2, 3, 4)), optimal, 4);
        assertSequence(results(result("A", 0, 1, 2, 3)), optimal, 5);
        assertSequence(results(result("B", 1, 2, 3)), optimal, 6);
        assertSequence(results(result("C", 2, 3)), optimal, 7);
        assertSequence(results(result("D", 3)), optimal, 8);
    }

    @Test
    public void testStackOverflow() {
        var choices = choices( //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "Of course", //
                "No Miss, of course not", //
                "No, of course not, Miss", //
                "No, of course not", //
                "Of course not");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // TODO correctly symbolized but then sliced as
        // slice 2 = "Miss of course"=[0, 4] "of course"=[1, 2, 3, 5, 6, 7]
        // it's a slicing bug, should be s=5, d=1, m=8
        assertEquals(4, optimal.size());
        assertEquals(2, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testOccursLaterInAnotherSequenceDisjunct() {
        var choices = choices( //
                "a B c x", //
                "e B g h", //
                "i D j k", //
                "l D m n", //
                "o p q D" //
        ).joinDuplicates();

        assertEquals(choice("a", "B", "c x"), choices.get(0));
        assertEquals(choice("e", "B", "g h"), choices.get(1));
        assertEquals(choice("i", "D", "j k"), choices.get(2));
        assertEquals(choice("l", "D", "m n"), choices.get(3));
        assertEquals(choice("o p q", "D"), choices.get(4));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(3, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testOccursLaterInAnotherSequenceShort() {
        var choices = choices( //
                "L D m n", //
                "o L q D" //
        ).joinDuplicates();

        assertEquals(choice("L", "D", "m n"), choices.get(0));
        assertEquals(choice("o", "L", "q", "D"), choices.get(1));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(5, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testOccursLaterInAnotherSequence() {
        var choices = choices( //
                "a B c x", //
                "e B g h", //
                "i D j k", //
                "L D m n", //
                "o L q D" //
        ).joinDuplicates();

        assertEquals(choice("a", "B", "c x"), choices.get(0));
        assertEquals(choice("e", "B", "g h"), choices.get(1));
        assertEquals(choice("i", "D", "j k"), choices.get(2));
        assertEquals(choice("L", "D", "m n"), choices.get(3));
        assertEquals(choice("o", "L", "q", "D"), choices.get(4));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(5, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation() {
        var choices = choices( //
                "Yes, My Dearest Mistress", //
                "Yes, Dearest Mistress", //
                "Yes, Mistress", //
                "Yessir, My Dearest Mistress", //
                "Yessir, Dearest Mistress", //
                "Yessir, Mistress", //
                "Yessir" //
        ).joinDuplicates();

        assertEquals(choice("Yes", "My", "Dearest", "Mistress"), choices.get(0));
        assertEquals(choice("Yes", "Dearest", "Mistress"), choices.get(1));
        assertEquals(choice("Yes", "Mistress"), choices.get(2));
        assertEquals(choice("Yessir", "My", "Dearest", "Mistress"), choices.get(3));
        assertEquals(choice("Yessir", "Dearest", "Mistress"), choices.get(4));
        assertEquals(choice("Yessir", "Mistress"), choices.get(5));
        assertEquals(choice("Yessir"), choices.get(6));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation2() {
        var choices = choices( //
                "Yes, My Dearest Mistress", //
                "Yes, My Dearest", //
                "Yes, My", //
                "Yessir, My Dearest Mistress", //
                "Yessir, My Dearest", //
                "Yessir, My", //
                "Yessir" //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation3() {
        var choices = choices( //
                "Yes, My Dearest Mistress", //
                "Yes, Dearest Mistress", //
                "Yes, Dearest", //
                "Yessir, My Dearest Mistress", //
                "Yessir, Dearest Mistress", //
                "Yessir, Dearest", //
                "Yessir" //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // TODO result contains 1 duplicated symbol although a 4-size slice without duplicates is possible
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testLongComputeTimeAndOutOfMemory() {
        var choices = choices( //
                "A B, F G H", //
                "A B, G H", //
                "A B, G", //
                "A B", //
                "C, F G H, A B", //
                "C, G H, A B", //
                "C, G, A B", //
                "U V W X Y Z, G H");
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(7, optimal.rating.maxCommonness);

        assertSequence(results(result("U", 7), result("A B", 0, 1, 2, 3), result("C", 4, 5, 6)), optimal, 0);
        assertSequence(results(result("F", 0, 4), result("V", "W", "X", "Y", "Z", 7)), optimal, 1);
        assertSequence(results(result("G", 0, 1, 2, 4, 5, 6, 7)), optimal, 2);
        assertSequence(results(result("H", 0, 1, 4, 5, 7)), optimal, 3);
        assertSequence(results(result("A B", 4, 5, 6)), optimal, 4);
    }

    @Test
    public void testDifferentSuccessorNotUsedByOther() {
        var choices = choices( //
                "A B D", //
                "A C E").joinDuplicates();

        assertEquals(choice("A", "B D"), choices.get(0));
        assertEquals(choice("A", "C E"), choices.get(1));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result("A", 0, 1)), optimal, 0);
        assertSequence(results(result("B D", 0), result("C E", 1)), optimal, 1);
    }

    @Test
    public void testDifferentSuccessorUsedByOther() {
        var choices = choices( //
                "A B C", //
                "A C E").joinDuplicates();

        assertEquals(choice("A", "B", "C"), choices.get(0));
        assertEquals(choice("A", "C", "E"), choices.get(1));

        SlicedPhrases<PhraseString> optimal = slice(choices.toPhraseStringSequences());

        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result("A", 0, 1)), optimal, 0);
        assertSequence(results(result("B", 0)), optimal, 1);
        assertSequence(results(result("C", 0, 1)), optimal, 2);
        assertSequence(results(result("E", 1)), optimal, 3);
    }

}
