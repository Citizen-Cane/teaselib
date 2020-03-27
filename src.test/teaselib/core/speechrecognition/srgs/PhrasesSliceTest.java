package teaselib.core.speechrecognition.srgs;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static teaselib.core.speechrecognition.srgs.PhraseString.Traits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhrasesSliceTest {
    private static final Logger logger = LoggerFactory.getLogger(PhrasesSliceTest.class);

    static Sequence<PhraseString> choice(String string, Integer... choices) {
        return Sequence.of(new PhraseString(string, Stream.of(choices).collect(toSet())), Traits);
    }

    static Sequence<PhraseString> result(String string, Integer... choices) {
        return new Sequence<>(singletonList(new PhraseString(string, Stream.of(choices).collect(toSet()))), Traits);
    }

    final List<List<Sequences<PhraseString>>> candidates = new ArrayList<>();

    private static SlicedPhrases<PhraseString> slice(PhraseStringSequences choices) {
        List<SlicedPhrases<PhraseString>> candidates = new ArrayList<>();
        long start = System.currentTimeMillis();
        SlicedPhrases<PhraseString> optimal = SlicedPhrases.of(choices, candidates);
        long end = System.currentTimeMillis();
        logger.info("Slicing duration = {}ms", end - start);

        assertEquals("Distinct symbol count", Collections.singletonList((int) optimal.distinctSymbolsCount()),
                candidates.stream().map(e -> e.rating.symbols.size()).distinct().collect(Collectors.toList()));

        for (SlicedPhrases<PhraseString> candidate : candidates) {
            assertEquals(candidate.toString() + "\t duplicates", candidate.duplicatedSymbolsCount(),
                    candidate.rating.duplicatedSymbols);

            assertEquals(candidate.toString() + "\t max commonness", candidate.maxCommonness(),
                    candidate.rating.maxCommonness);
        }

        return optimal;
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
    public void testCommonStart() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, B", 0), choice("A, C", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(choice("A", 0, 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(choice("B", 0), choice("C", 1)), optimal.get(1));
        assertEquals(2, optimal.size());
    }

    @Test
    public void testCommonEnd() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, C", 0), choice("B, C", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(choice("A", 0), choice("B", 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(choice("C", 0, 1)), optimal.get(1));
        assertEquals(2, optimal.size());
    }

    @Test
    public void testThatChoiceStringYes3() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("yes Miss, of course", 0),
                choice("Yes, of Course, Miss", 1), choice("Of course", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("Yes", 0, 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Miss", 0)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("Miss", 1)), optimal.get(3));
        assertEquals(4, optimal.size());
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

        assertEquals(new PhraseStringSequences(result("Yes", 0, 1, 2), result("No", 3, 4, 5)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2, 3, 4, 5)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("not", 3, 4, 5)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("Miss", 1, 4)), optimal.get(4));
        assertEquals(5, optimal.size());
    }

    @Test
    public void testThatSliceCommonPrefersLargerCommonChunks() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Miss, of course", 0), //
                choice("of Course, Miss", 1), //
                choice("of course", 2), //
                choice("Miss, of course, Miss", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("Miss", 0, 3)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("Miss", 1, 3)), optimal.get(2));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStringsOneOptionalPart() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Miss, of course", 0), //
                choice("No, of Course not, Miss", 1));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("yes Miss", 0), result("No", 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("not Miss", 1)), optimal.get(2));
        assertEquals(3, optimal.size());
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

        assertEquals(new PhraseStringSequences(result("No", 0), result("I have it", 2), result("Yes", 1, 3)),
                optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 1, 2), result("it's", 3, 4)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1, 3, 4)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("Miss", 3, 4)), optimal.get(4));
        assertEquals(5, optimal.size());
    }

    @Test
    public void testSliceMultipleCommon1() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B0 C0 D", 0), //
                choice("A B1 C0 D", 1), //
                choice("A B2 C2 D", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1, 2)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("b0", 0), result("b1", 1), result("b2 c2", 2)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("c0", 0, 1)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("d", 0, 1, 2)), optimal.get(3));
        assertEquals(4, optimal.size());
    }

    @Test
    public void testSliceMultipleCommon2() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B0 C0 D", 0), //
                choice("A B1 C0 D", 1), //
                choice("A B2 C2 D", 2), //
                choice("A B3 C2 D", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1, 2, 3)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("b0", 0), result("b1", 1), result("b2", 2), result("b3", 3)),
                optimal.get(1));
        assertEquals(new PhraseStringSequences(result("c0", 0, 1), result("c2", 2, 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("d", 0, 1, 2, 3)), optimal.get(3));
        assertEquals(4, optimal.size());
    }

    @Test
    public void testPunctationMarksAreIgnored() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C", 0), //
                choice("A,B C.", 1), //
                choice("a B,C.", 2));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("A B C", 0, 1, 2)), optimal.get(0));
        assertEquals(1, optimal.size());
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

        assertEquals(new PhraseStringSequences(//
                result("N", 5), result("A", 0, 3), result("D", 1, 2, 4)), optimal.get(0));
        assertEquals(new PhraseStringSequences(//
                result("B C", 0, 5)), optimal.get(1));
        assertEquals(new PhraseStringSequences(//
                result("O", 5), result("D", 0, 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(//
                result("E F", 0, 1), result("I", 2, 3, 4)), optimal.get(3));
        assertEquals(new PhraseStringSequences(//
                result("G", 0), result("H", 1), result("J", 2), result("K L", 3), result("F M", 4)), optimal.get(4));
        assertEquals(new PhraseStringSequences(//
                result("B C", 1, 2, 3, 4)), optimal.get(5));
        assertEquals(6, optimal.size());
    }

    @Test
    public void testSliceSplitShort() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B", 0), //
                choice("A B", 1), //
                choice("B", 2), //
                choice("B", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("B", 0, 1, 2, 3)), optimal.get(1));
        assertEquals(2, optimal.size());
    }

    @Test
    public void testSliceSplit() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C D", 0), //
                choice("A B C E", 1), //
                choice("B C F", 2), //
                choice("B C G", 3));
        SlicedPhrases<PhraseString> optimal = slice(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("B C", 0, 1, 2, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("D", 0), result("E", 1), result("F", 2), result("G", 3)),
                optimal.get(2));
        assertEquals(3, optimal.size());
    }

    @Test
    public void testSliceMultipleCommon4() {
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

        assertEquals(new PhraseStringSequences(result("A", 0, 1, 8, 10)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("B C", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)), optimal.get(1));
        assertEquals(7, optimal.size());
    }

}