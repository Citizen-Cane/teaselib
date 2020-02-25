package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static teaselib.core.speechrecognition.srgs.Sequences.averageCommonness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

public class PhrasesSliceTest {

    static Sequence<PhraseString> choice(String string, Integer... choices) {
        return new Sequence<>(new PhraseString(string, Stream.of(choices).collect(toSet())).words(),
                PhraseString.Traits);
    }

    static Sequence<PhraseString> result(String string, Integer... choices) {
        return new Sequence<>(Collections.singletonList(new PhraseString(string, Stream.of(choices).collect(toSet()))),
                PhraseString.Traits);
    }

    final List<List<Sequences<PhraseString>>> candidates = new ArrayList<>();
    final DebugPhraseStringSequencesList soFar = new DebugPhraseStringSequencesList();

    private Sequences<PhraseString> advance(PhraseStringSequences choices) {
        DebugPhraseStringSequences slice = new DebugPhraseStringSequences(choices.slice(candidates, soFar));
        soFar.add(slice);
        return slice;
    }

    private DebugPhraseStringSequencesList complete(PhraseStringSequences choices) {
        while (!advance(choices).isEmpty()) { //
        }
        return soFar;
    }

    @Test
    public void testCommonStart() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, B", 0), choice("A, C", 1));

        Sequences<PhraseString> common = advance(choices);
        assertEquals(new PhraseStringSequences(choice("A", 0, 1)), common);

        Sequences<PhraseString> disjunct = advance(choices);
        assertEquals(new PhraseStringSequences(choice("B", 0), choice("C", 1)), disjunct);

        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testCommonEnd() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, C", 0), choice("B, C", 1));

        Sequences<PhraseString> disjunct = advance(choices);
        assertEquals(new PhraseStringSequences(choice("A", 0), choice("B", 1)), disjunct);

        Sequences<PhraseString> common = advance(choices);
        assertEquals(new PhraseStringSequences(choice("C", 0, 1)), common);

        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testThatChoiceStringYes3() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("yes Miss, of course", 0),
                choice("Yes, of Course, Miss", 1), choice("Of course", 2));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Yes", 0, 1)), slice1);

        Sequences<PhraseString> slice2 = advance(choices);
        assertFalse(slice2.isEmpty());

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);

        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertNotEquals(optimal, subOptimal);
        assertEquals(6, Sequences.averageCommonness(optimal));
        assertEquals(6, Sequences.averageCommonness(subOptimal));
    }

    @Test
    public void testThatCommonSlicesConsiderSubsequentCommonSlices() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("yes Miss, of course", 0), choice("Yes, Of Course, Miss", 1), choice("Yes, of course", 2), //
                choice("No Miss, of course not", 3), choice("No, of Course not, Miss", 4),
                choice("No, Of course not", 5));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Yes", 0, 1, 2), result("No", 3, 4, 5)), slice1);

        Sequences<PhraseString> slice2 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Miss of", 0, 3), result("of course", 1, 2, 4, 5)), slice2);

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertNotEquals(optimal, subOptimal);
        assertEquals(19, Sequences.averageCommonness(optimal));
        assertEquals(17, Sequences.averageCommonness(subOptimal));

        assertEquals(new PhraseStringSequences(result("Yes", 0, 1, 2), result("No", 3, 4, 5)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2, 3, 4, 5)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("not", 3, 4, 5)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("Miss", 1, 4)), optimal.get(4));
    }

    @Test
    public void testThatSliceCommonPrefersLargerCommonChunks() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("Miss, of course", 0), choice("of Course, Miss", 1), choice("of course", 2),
                choice("Miss, of course, Miss", 3));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Miss of course", 0, 3), result("of course", 1, 2)), slice1);

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertNotEquals(optimal, subOptimal);
        assertEquals(9, Sequences.averageCommonness(optimal));
        assertEquals(9, Sequences.averageCommonness(subOptimal));

        assertEquals(new PhraseStringSequences(result("Miss", 0, 3)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("Miss", 1, 3)), optimal.get(2));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStringsOneOptionalPart() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("yes Miss, of course", 0),
                choice("No, of Course not, Miss", 1));

        Sequences<PhraseString> slice1 = advance(choices);
        Sequences<PhraseString> slice2 = advance(choices);
        Sequences<PhraseString> slice3 = advance(choices);
        Sequences<PhraseString> empty = advance(choices);

        assertEquals(new PhraseStringSequences(result("yes Miss", 0), result("No", 1)), slice1);
        assertEquals(new PhraseStringSequences(result("of course", 0, 1)), slice2);
        assertEquals(new PhraseStringSequences(result("not Miss", 1)), slice3);

        assertTrue(empty.isEmpty());
    }

    private static PhraseStringSequences getMultipleChoiceIrregularPhrases() {
        return new PhraseStringSequences(choice("No Miss, I'm sorry", 0), choice("Yes Miss, I'm ready", 1),
                choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3), choice("It's ready, Miss", 4));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesTryout() {
        PhraseStringSequences choices = getMultipleChoiceIrregularPhrases();

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertEquals(optimal, subOptimal);
        assertEquals(10, Sequences.averageCommonness(optimal));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesStraightSubOptimal() {
        PhraseStringSequences choices = getMultipleChoiceIrregularPhrases();

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("No", 0), result("I have it", 2)), slice1);
        Sequences<PhraseString> slice2 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Miss", 0, 2), result("Yes", 1, 3)), slice2);
        Sequences<PhraseString> slice3 = advance(choices);
        assertEquals(new PhraseStringSequences(result("It's ready", 3, 4)), slice3);
        Sequences<PhraseString> slice4 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Miss", 1, 3, 4)), slice4);
        Sequences<PhraseString> slice5 = advance(choices);
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), slice5);
        Sequences<PhraseString> slice6 = advance(choices);
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1)), slice6);

        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSliceMultipleCommon1() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A B0 C0 D", 0), choice("A B1 C0 D", 1),
                choice("A B2 C2 D", 2));

        Sequences<PhraseString> slice1 = advance(choices);
        Sequences<PhraseString> slice2 = advance(choices);
        Sequences<PhraseString> slice3 = advance(choices);
        Sequences<PhraseString> slice4 = advance(choices);
        Sequences<PhraseString> empty = advance(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1, 2)), slice1);
        assertEquals(new PhraseStringSequences(result("b0", 0), result("b1", 1), result("b2 c2", 2)), slice2);
        assertEquals(new PhraseStringSequences(result("c0", 0, 1)), slice3);
        assertEquals(new PhraseStringSequences(result("d", 0, 1, 2)), slice4);

        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSliceMultipleCommon2() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A B0 C0 D", 0), choice("A B1 C0 D", 1),
                choice("A B2 C2 D", 2), choice("A B3 C2 D", 3));

        Sequences<PhraseString> slice1 = advance(choices);
        Sequences<PhraseString> slice2 = advance(choices);
        Sequences<PhraseString> slice3 = advance(choices);
        Sequences<PhraseString> slice4 = advance(choices);
        Sequences<PhraseString> empty = advance(choices);

        assertEquals(new PhraseStringSequences(result("A", 0, 1, 2, 3)), slice1);
        assertEquals(new PhraseStringSequences(result("b0", 0), result("b1", 1), result("b2", 2), result("b3", 3)),
                slice2);
        assertEquals(new PhraseStringSequences(result("c0", 0, 1), result("c2", 2, 3)), slice3);
        assertEquals(new PhraseStringSequences(result("d", 0, 1, 2, 3)), slice4);

        assertTrue(empty.isEmpty());
    }

    @Test
    public void testPunctationMarksIndependence() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A B C", 0), choice("A,B C.", 1),
                choice("a B,C.", 2));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("a b c", 0, 1, 2)), slice1);
        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        assertEquals(1, candidates.size());

        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertEquals(8, Sequences.averageCommonness(optimal));
    }

    @Test
    public void testSliceMultipleCommon3() {
        // D is correctly split between phrase 1 and 4 to allow A in phrase 1 to be distinct
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C, D E F G", 0), //
                choice("D E F H, B C", 1), //
                choice("D I J, B C", 2), //
                choice("A, D I K L, B C", 3), //
                choice("D I F M, B C", 4), //
                choice("N B C, O", 5));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        DebugPhraseStringSequencesList optimal = new DebugPhraseStringSequencesList(Sequences.reduce(candidates));
        assertEquals(18, Sequences.averageCommonness(subOptimal));
        assertEquals(20, Sequences.averageCommonness(optimal));

        List<DebugPhraseStringSequencesList> allOptimal = candidates.stream().filter(s -> averageCommonness(s) >= 20)
                .map(DebugPhraseStringSequencesList::new).collect(toList());

        assertEquals(4, allOptimal.size());

        assertEquals(8, allOptimal.get(0).size());
        assertEquals(20, Sequences.averageCommonness(allOptimal.get(0)));
        assertEquals(7, allOptimal.get(1).size());
        assertEquals(20, Sequences.averageCommonness(allOptimal.get(1)));
        assertEquals(8, allOptimal.get(2).size());
        assertEquals(20, Sequences.averageCommonness(allOptimal.get(2)));
        assertEquals(10, allOptimal.get(3).size());
        assertEquals(20, Sequences.averageCommonness(allOptimal.get(3)));

        assertEquals(7, optimal.size());
    }

}