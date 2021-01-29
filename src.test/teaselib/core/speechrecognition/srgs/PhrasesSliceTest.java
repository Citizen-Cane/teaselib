package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;
import static teaselib.core.speechrecognition.srgs.PhraseString.*;
import static teaselib.core.speechrecognition.srgs.PhraseStringSequences.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhrasesSliceTest {
    private static final Logger logger = LoggerFactory.getLogger(PhrasesSliceTest.class);

    public static Sequence<PhraseString> choice(String string, Integer... choices) {
        return Sequence.of(new PhraseString(string, Stream.of(choices).collect(toSet())), Traits);
    }

    public static Sequence<PhraseString> result(String string, Integer... choices) {
        return new Sequence<>(singletonList(new PhraseString(string, Stream.of(choices).collect(toSet()))), Traits);
    }

    public static Sequence<PhraseString> result(List<String> strings, Integer... choices) {
        Set<Integer> indices = Stream.of(choices).collect(toSet());
        return new Sequence<>(strings.stream().map(phrase -> new PhraseString(phrase, indices)).collect(toList()),
                Traits);
    }

    @SafeVarargs
    private static PhraseStringSequences results(Sequence<PhraseString>... results) {
        return new PhraseStringSequences(results);
    }

    final List<List<Sequences<PhraseString>>> candidates = new ArrayList<>();

    public static SlicedPhrases<PhraseString> slice(PhraseStringSequences choices) {
        List<SlicedPhrases<PhraseString>> candidates = new ArrayList<>();
        long start = System.currentTimeMillis();
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(new PhraseStringSequences(choices), candidates,
                PhraseStringSequences::prettyPrint);
        long end = System.currentTimeMillis();
        logger.info("Slicing duration = {}ms", end - start);

        assertSliced(choices, optimal);

        assertEquals("Distinct symbol count", Collections.singletonList((int) optimal.distinctSymbolsCount()),
                candidates.stream().map(e -> e.rating.symbols.size()).distinct().collect(toList()));

        for (SlicedPhrases<PhraseString> candidate : candidates) {
            assertEquals(candidate.toString() + "\t duplicates", candidate.duplicatedSymbolsCount(),
                    candidate.rating.duplicatedSymbols);

            assertEquals(candidate.toString() + "\t max commonness", candidate.maxCommonness(),
                    candidate.rating.maxCommonness);

            assertEquals(candidate.toString() + "\t max commonness", candidate.maxCommonness(),
                    candidate.rating.maxCommonness);

            assertEquals(candidate.toString() + "\t all symbols mut be split", 1, candidate.rating.symbols.stream()
                    .map(e -> e.toString().split(" ").length).distinct().collect(toSet()).size());
        }

        return optimal;
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D", 0), //
                choice("B C D", 1), //
                choice("C D", 2), //
                choice("D", 3), //
                choice("D E", 4));
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D", 0), //
                choice("B C D", 1), //
                choice("C D", 2), //
                choice("D", 3));
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A", 0), //
                choice("A B", 1), //
                choice("A B C", 2), //
                choice("A B C D", 3));
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B0 C0 D", 0), //
                choice("A B1 C0 D", 1), //
                choice("A B2 C2 D", 2));
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D", 0), //
                choice("A B C", 1), //
                choice("A B", 2), //
                choice("A", 3)); //
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A, B", 0), //
                choice("A, C", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(choice("A", 0, 1)), optimal, 0);
        assertSequence(results(choice("B", 0), choice("C", 1)), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testCommonEnd() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A, C", 0), //
                choice("B, C", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(choice("A", 0), choice("B", 1)), optimal, 0);
        assertSequence(results(choice("C", 0, 1)), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testYes3() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Miss, of course", 0), //
                choice("Yes, of Course, Miss", 1), //
                choice("Of course", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("Yes", 0, 1)), optimal, 0);
        assertSequence(results(result("Miss", 0)), optimal, 1);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1, 2)), optimal, 2);
        assertSequence(results(result("Miss", 1)), optimal, 3);
        assertEquals(4, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testYes3b() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Miss, of course", 0), //
                choice("Yes, of Course, Miss", 1), //
                choice("Of course", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("Miss", 0), result("Yes", 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1, 2)), optimal, 1);
        assertSequence(results(result("Miss", 1)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testThatCommonSlicesConsiderSubsequentCommonSlices() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Miss, of course", 0), //
                choice("Yes, Of Course, Miss", 1), //
                choice("Yes, of course", 2), //
                choice("No Miss, of course not", 3), //
                choice("No, of Course not, Miss", 4), //
                choice("No, Of course not", 5));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("No", 3, 4, 5), result("Yes", 0, 1, 2)), optimal, 0);
        assertSequence(results(result("Miss", 0, 3)), optimal, 1);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1, 2, 3, 4, 5)), optimal, 2);
        assertSequence(results(result("not", 3, 4, 5)), optimal, 3);
        assertSequence(results(result("Miss", 1, 4)), optimal, 4);
        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testThatSliceCommonPrefersLargerCommonChunks() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Miss, of course", 0), //
                choice("of Course, Miss", 1), //
                choice("of course", 2), //
                choice("Miss, of course, Miss", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("Miss", 0, 3)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1, 2, 3)), optimal, 1);
        assertSequence(results(result("Miss", 1, 3)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance1() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Miss, of course", 0), //
                choice("No, of Course not, Miss", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result(Arrays.asList("yes", "Miss"), 0), result("No", 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Miss"), 1)), optimal, 2);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance1Reverse() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("No, of Course not, Miss", 0), //
                choice("yes Miss, of course", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);

        assertSequence(results(result("No", 0), result(Arrays.asList("yes", "Miss"), 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Miss"), 0)), optimal, 2);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance2() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Mistress Lee, of course", 0), //
                choice("No, of Course not, Mistress", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result(Arrays.asList("yes", "Mistress", "Lee"), 0), result("No", 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Mistress"), 1)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance2Reverse() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("No, of Course not, Mistress", 0), //
                choice("yes Mistress Lee, of course", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("No", 0), result(Arrays.asList("yes", "Mistress", "Lee"), 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Mistress"), 0)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance3() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Mistress Lee La, of course", 0), //
                choice("No, of Course not, Mistress", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result(Arrays.asList("yes", "Mistress", "Lee", "La"), 0), result("No", 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Mistress"), 1)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceAllDisjunctPartOccurLaterInAnotherSequenceAtDistance3Reverse() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("No, of Course not, Mistress", 0), //
                choice("yes Mistress Lee La, of course", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("No", 0), result(Arrays.asList("yes", "Mistress", "Lee", "La"), 1)), optimal, 0);
        assertSequence(results(result(Arrays.asList("of", "course"), 0, 1)), optimal, 1);
        assertSequence(results(result(Arrays.asList("not", "Mistress"), 0)), optimal, 2);

        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(2, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesMixedCase() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("No Miss, I'm sorry", 0), //
                choice("Yes Miss, I'm ready", 1), //
                choice("I have it, Miss", 2), //
                choice("Yes,it's ready, Miss", 3), //
                choice("It's ready, Miss", 4));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(4, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);

        // "I have it" is eventually sliced and moved, but pushes common "Miss" to match at the end
        assertSequence(results(result("No", 0), result("I", 2), result("Yes", 1, 3)), optimal, 0);
        assertSequence(results(result(asList("Miss", "I'm"), 0, 1), result("it's", 3, 4), result("have", 2)), optimal,
                1);
        assertSequence(results(result("ready", 1, 3, 4), result("sorry", 0), result("it", 2)), optimal, 2);
        assertSequence(results(result("Miss", 2, 3, 4)), optimal, 3);
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesSingleDisjunctSymbol() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("No Miss, I'm sorry", 0), //
                choice("Yes Miss, I'm ready", 1), //
                choice("Okay, Miss", 2), //
                choice("Yes,it's ready, Miss", 3), //
                choice("It's ready, Miss", 4));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        // "okay" is sliced immediately which makes common "Miss" to match next
        assertSequence(results(result("Yes", 1, 3), result("No", 0), result("Okay", 2)), optimal, 0);
        assertSequence(results(result(Arrays.asList("It's", "ready"), 3, 4)), optimal, 1);
        assertSequence(results(result(Arrays.asList("Miss"), 0, 1, 2, 3, 4)), optimal, 2);
        assertSequence(results(result("I'm", 0, 1)), optimal, 3);
        assertSequence(results(result("sorry", 0), result("ready", 1)), optimal, 4);
    }

    @Test
    public void testSliceMultipleCommon1ReduceNullRulesProduction() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B0 C0 D", 0), //
                choice("A B1 C0 D", 1), //
                choice("A B2 C2 D", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B0 C0 D", 0), //
                choice("A B1 C0 D", 1), //
                choice("A B2 C2 D", 2), //
                choice("A B3 C2 D", 3));
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
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C", 0), //
                choice("A,B C.", 1), //
                choice("a B,C.", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result(Arrays.asList("A", "B", "C"), 0, 1, 2)), optimal, 0);
        assertEquals(1, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon3() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C, D E F G", 0), //
                choice("D E F H, B C", 1), //
                choice("D I J, B C", 2), //
                choice("A, D I K L, B C", 3), //
                choice("D I F M, B C", 4), //
                choice("N B C, O", 5));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(results(result("A", 0, 3), result("N", 5)), optimal, 0);
        assertSequence(result(Arrays.asList("B", "C"), 0, 5), optimal, 1);
        assertSequence(results(result("O", 5), result("D", 0, 1, 2, 3, 4)), optimal, 2);
        assertSequence(results(//
                result(Arrays.asList("E", "F"), 0, 1), result("I", 2, 3, 4)), optimal, 3);
        assertSequence(results(//
                result("G", 0), result("H", 1), result("J", 2), //
                result(Arrays.asList("K", "L"), 3), //
                result(Arrays.asList("F", "M"), 4)), optimal, 4);
        assertSequence(results(//
                result(Arrays.asList("B", "C"), 1, 2, 3, 4)), optimal, 5);
        assertEquals(6, optimal.size());
        assertEquals(3, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);
    }

    public static void main(String argv[]) {
        new PhrasesSliceTest().testSliceMultipleCommon3Performance();
    }

    // @Test
    public void testSliceMultipleCommon3Performance() {
        SlicedPhrases<PhraseString> optimal = null;

        long start = System.currentTimeMillis();
        int n = 20;
        for (int i = 0; i < n; i++) {
            PhraseStringSequences choices = new PhraseStringSequences( //
                    choice("A B C, D E F G", 0), //
                    choice("D E F H, B C", 1), //
                    choice("D I J, B C", 2), //
                    choice("A, D I K L, B C", 3), //
                    choice("D I F M, B C", 4), //
                    choice("N B C, O", 5));
            long now = System.currentTimeMillis();
            optimal = SlicedPhrases.of(choices);
            long finish = System.currentTimeMillis();
            logger.info("result = {}ms", finish - now);
        }
        long end = System.currentTimeMillis();
        logger.info("---------------------------------");
        logger.info("Overall = {}ms", end - start);
        logger.info("average = {}ms", (double) (end - start) / n);

        assertNotNull(optimal);
    }

    @Test
    public void testSliceSplitShort() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B", 0), //
                choice("A B", 1), //
                choice("B", 2), //
                choice("B", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1), optimal, 0);
        assertSequence(result("B", 0, 1, 2, 3), optimal, 1);
        assertEquals(2, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceSplit() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D", 0), //
                choice("A B C E", 1), //
                choice("B C F", 2), //
                choice("B C G", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1), optimal, 0);
        assertSequence(result(Arrays.asList("B", "C"), 0, 1, 2, 3), optimal, 1);
        assertSequence(results(result("D", 0), result("E", 1), result("F", 2), result("G", 3)), optimal, 2);
        assertEquals(3, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon4() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D E?", 0), //
                choice("A B C F G H?", 1), //
                choice("B C II F J K", 2), //
                choice("B C L M N O P", 3), //
                choice("B C L M Q RT", 4), //
                choice("B C L M Q ST", 5), //
                choice("B C II F J UV", 6), //
                choice("B C W F N X Y U", 7), //
                choice("A B C X Y U Z", 8), //
                choice("B C 1 2 34 5", 9), //
                choice("A B C F 6 7", 10));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1, 8, 10), optimal, 0);
        assertSequence(result(Arrays.asList("B", "C"), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), optimal, 1);
        assertEquals(7, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(11, optimal.rating.maxCommonness);
    }

    @Test
    public void testSliceMultipleCommon4Hyphenation() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D E?", 0), //
                choice("A B C F G H?", 1), //
                choice("B C II F J K", 2), //
                choice("B C L M N O P", 3), //
                choice("B C L M Q R-T", 4), //
                choice("B C L M Q S-T", 5), //
                choice("B C II F J U-V", 6), //
                choice("B C W F N X Y U", 7), //
                choice("A B C X Y U Z", 8), //
                choice("B C 1 2 3-4 5", 9), //
                choice("A B C F 6 7", 10));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertSequence(result("A", 0, 1, 8, 10), optimal, 0);
        assertSequence(result(Arrays.asList("B", "C"), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), optimal, 1);
        assertEquals(8, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(11, optimal.rating.maxCommonness);
    }

    @Test
    public void testUnsliceableSliceOrderInvariance() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D E", 0), //
                choice("E A B C D", 1), //
                choice("D E A B C", 2), //
                choice("C D E A B", 3), //
                choice("B C D E A", 4));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(9, optimal.size());
        assertEquals(4, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        assertSequence(results(result(Arrays.asList("A"), 0)), optimal, 0);
        assertSequence(results(result(Arrays.asList("B"), 0, 4)), optimal, 1);
        assertSequence(results(result(Arrays.asList("C"), 0, 3, 4)), optimal, 2);
        assertSequence(results(result(Arrays.asList("D"), 0, 2, 3, 4)), optimal, 3);
        assertSequence(results(result(Arrays.asList("E"), 0, 1, 2, 3, 4)), optimal, 4);
        assertSequence(results(result(Arrays.asList("A"), 1, 2, 3, 4)), optimal, 5);
        assertSequence(results(result(Arrays.asList("B"), 1, 2, 3)), optimal, 6);
        assertSequence(results(result(Arrays.asList("C"), 1, 2)), optimal, 7);
        assertSequence(results(result(Arrays.asList("D"), 1)), optimal, 8);
    }

    @Test
    public void testUnsliceableSliceOrderInvarianceReverse() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("B C D E A", 0), //
                choice("C D E A B", 1), //
                choice("D E A B C", 2), //
                choice("E A B C D", 3), //
                choice("A B C D E", 4));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(9, optimal.size());
        assertEquals(4, optimal.rating.duplicatedSymbols);
        assertEquals(5, optimal.rating.maxCommonness);

        assertSequence(results(result(Arrays.asList("A"), 4)), optimal, 0);
        assertSequence(results(result(Arrays.asList("B"), 0, 4)), optimal, 1);
        assertSequence(results(result(Arrays.asList("C"), 0, 1, 4)), optimal, 2);
        assertSequence(results(result(Arrays.asList("D"), 0, 1, 2, 4)), optimal, 3);
        assertSequence(results(result(Arrays.asList("E"), 0, 1, 2, 3, 4)), optimal, 4);
        assertSequence(results(result(Arrays.asList("A"), 0, 1, 2, 3)), optimal, 5);
        assertSequence(results(result(Arrays.asList("B"), 1, 2, 3)), optimal, 6);
        assertSequence(results(result(Arrays.asList("C"), 2, 3)), optimal, 7);
        assertSequence(results(result(Arrays.asList("D"), 3)), optimal, 8);
    }

    @Test
    public void testStackOverflow() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Yes Miss, of course", 0), //
                choice("Yes, of course, Miss", 1), //
                choice("Yes, of course", 2), //
                choice("Of course", 3), //
                choice("No Miss, of course not", 4), //
                choice("No, of course not, Miss", 5), //
                choice("No, of course not", 6), //
                choice("Of course not", 7));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(5, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(8, optimal.rating.maxCommonness);
    }

    @Test
    public void testOccursLaterInAnotherSequenceDisjunct() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("a B c x", 0), //
                choice("e B g h", 1), //
                choice("i D j k", 2), //
                choice("l D m n", 3), //
                choice("o p q D", 4) //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(3, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testOccursLaterInAnotherSequence() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("a B c x", 0), //
                choice("e B g h", 1), //
                choice("i D j k", 2), //
                choice("L D m n", 3), //
                choice("o L q D", 4) //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(5, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(3, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Yes, My Dearest Mistress", 0), //
                choice("Yes, Dearest Mistress", 1), //
                choice("Yes, Mistress", 2), //
                choice("Yessir, My Dearest Mistress", 3), //
                choice("Yessir, Dearest Mistress", 4), //
                choice("Yessir, Mistress", 5), //
                choice("Yessir", 6) //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation2() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Yes, My Dearest Mistress", 0), //
                choice("Yes, My Dearest", 1), //
                choice("Yes, My", 2), //
                choice("Yessir, My Dearest Mistress", 3), //
                choice("Yessir, My Dearest", 4), //
                choice("Yessir, My", 5), //
                choice("Yessir", 6) //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(4, optimal.size());
        assertEquals(0, optimal.rating.duplicatedSymbols);
        assertEquals(6, optimal.rating.maxCommonness);
    }

    @Test
    public void testPunctation3() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Yes, My Dearest Mistress", 0), //
                choice("Yes, Dearest Mistress", 1), //
                choice("Yes, Dearest", 2), //
                choice("Yessir, My Dearest Mistress", 3), //
                choice("Yessir, Dearest Mistress", 4), //
                choice("Yessir, Dearest", 5), //
                choice("Yessir", 6) //
        );
        SlicedPhrases<PhraseString> optimal = slice(choices);

        // TODO result contains 1 duplicated symbol although a 4-size slice without duplicates is possible
        assertEquals(3, optimal.size());
        assertEquals(1, optimal.rating.duplicatedSymbols);
        assertEquals(4, optimal.rating.maxCommonness);
    }

}
