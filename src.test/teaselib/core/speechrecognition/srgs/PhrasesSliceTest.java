package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.Ignore;
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
    final List<Sequences<PhraseString>> soFar = new ArrayList<>();

    private Sequences<PhraseString> advance(PhraseStringSequences choices) {
        Sequences<PhraseString> slice = choices.slice(candidates, soFar);
        soFar.add(slice);
        return slice;
    }

    private List<Sequences<PhraseString>> complete(PhraseStringSequences choices) {
        while (!advance(choices).isEmpty()) {
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
        assertEquals(3, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));
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
        assertEquals(6, Sequences.commonness(optimal));
        assertEquals(4, Sequences.commonness(subOptimal));

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
        assertEquals(4, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));

        assertEquals(new PhraseStringSequences(result("Miss", 0, 3)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("Miss", 1, 3)), optimal.get(2));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStringsOneOptionalPart() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("yes Miss, of course", 0),
                choice("No, of Course not, Miss", 1));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("yes Miss", 0), result("No", 1)), slice1);

        Sequences<PhraseString> slice2 = advance(choices);
        assertEquals(new PhraseStringSequences(result("of course", 0, 1)), slice2);

        Sequences<PhraseString> slice3 = advance(choices);
        assertEquals(new PhraseStringSequences(result("not Miss", 1)), slice3);

        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesTryout() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("It's ready, Miss", 4));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = Sequences.reduce(candidates);
        assertNotEquals(optimal, subOptimal);
        assertEquals(5, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesStraightSubOptimal() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("It's ready, Miss", 4));

        Sequences<PhraseString> slice1 = advance(choices);
        assertEquals(new PhraseStringSequences(result("No", 0)), slice1);
        Sequences<PhraseString> slice2 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Yes", 1, 3)), slice2);
        Sequences<PhraseString> slice3 = advance(choices);
        assertEquals(new PhraseStringSequences(result("Miss I'm", 0, 1), result("It's ready Miss", 3, 4)), slice3);
        Sequences<PhraseString> slice4 = advance(choices);
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1), result("I have it Miss", 2)),
                slice4);

        Sequences<PhraseString> empty = advance(choices);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesMaxCommon() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("It's ready, Miss", 4));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = candidates.stream().reduce((a, b) -> {
            int ca = Sequences.commonness(a);
            int cb = Sequences.commonness(b);
            return ca > cb ? a : b;
        }).orElseThrow();

        assertNotEquals(optimal, subOptimal);
        assertEquals(5, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));

        // TODO "I" + "have it" is split
        // TODO ~29073 candidates take long to compute
        // TODO It's ready could be matched -> candidate with less slices?
        assertEquals(10, optimal.size());
        assertEquals(new PhraseStringSequences(result("No", 0)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("It's", 4)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("Yes", 1, 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("It's", 3)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("ready", 3, 4)), optimal.get(4));
        assertEquals(new PhraseStringSequences(result("I", 2)), optimal.get(5));
        assertEquals(new PhraseStringSequences(result("have it", 2)), optimal.get(6));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 1, 2, 3, 4)), optimal.get(7));
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), optimal.get(8));
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1)), optimal.get(9));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesMaxCommonShortestSize() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("It's ready, Miss", 4));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = candidates.stream().reduce((a, b) -> {
            int ca = Sequences.commonness(a);
            int cb = Sequences.commonness(b);
            if (ca == cb) {
                return a.size() < b.size() ? a : b;
            } else {
                return ca > cb ? a : b;
            }
        }).orElseThrow();

        assertNotEquals(optimal, subOptimal);
        assertEquals(5, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));

        // TODO "I" + "have it" is split
        // TODO ~29073 candidates take long to compute
        // TODO It's ready could be matched -> candidate with less slices?
        assertEquals(9, optimal.size());
        assertEquals(new PhraseStringSequences(result("No", 0), result("It's", 4)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Yes", 1, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("It's", 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("ready", 3, 4)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("I", 2)), optimal.get(4));
        assertEquals(new PhraseStringSequences(result("have it", 2)), optimal.get(5));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 1, 2, 3, 4)), optimal.get(6));
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), optimal.get(7));
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1)), optimal.get(8));
    }

    @Test
    @Ignore
    public void testSliceMultipleChoiceIrregularPhrasesMaxCommonCrossProduct() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("It's ready, Miss", 4));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);
        List<Sequences<PhraseString>> optimal = candidates.stream().reduce((a, b) -> {
            Function<List<Sequences<PhraseString>>, Double> commonness = (slices) -> {
                int p = slices.stream().flatMap(Sequences::stream).flatMap(Sequence::stream).map(t -> t.indices.size())
                        .reduce(0, (x, y) -> x * x + y * y);
                return (double) p / slices.size();
            };
            double ca = commonness.apply(a);
            double cb = commonness.apply(b);
            if (ca == cb) {
                return a.size() < b.size() ? a : b;
            } else {
                return ca > cb ? a : b;
            }
        }).orElseThrow();

        assertNotEquals(optimal, subOptimal);
        assertEquals(5, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));

        // TODO "I have it" is split
        // TODO ~29073 candidates take long to compute
        // TODO It's ready could be matched -> candidate with less slices?
        assertEquals(9, optimal.size());
        assertEquals(new PhraseStringSequences(result("No", 0), result("It's", 4)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Yes", 1, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("It's", 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("ready", 3, 4)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("I", 2)), optimal.get(4));
        assertEquals(new PhraseStringSequences(result("have it", 2)), optimal.get(5));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 1, 2, 3, 4)), optimal.get(6));
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), optimal.get(7));
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1)), optimal.get(8));
    }

    @Test
    public void testSliceMultipleChoiceIrregularPhrasesMaxCommonMaxSmallest() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("No Miss, I'm sorry", 0),
                choice("Yes Miss, I'm ready", 1), choice("I have it, Miss", 2), choice("Yes,it's ready, Miss", 3),
                choice("it's ready, Miss", 4));

        List<Sequences<PhraseString>> subOptimal = complete(choices);
        candidates.add(subOptimal);

        Map<Integer, List<List<Sequences<PhraseString>>>> map = new LinkedHashMap<>();
        candidates.forEach(candidate -> {
            int key = Sequences.commonness(candidate);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(candidate);
        });

        List<List<Sequences<PhraseString>>> maxCandidates = new ArrayList<>();
        for (int i = max(map); i > 0; i--) {
            maxCandidates.addAll(map.get(i));
            break;
        }

        List<Sequences<PhraseString>> optimal = maxCandidates.get(0);
        assertNotEquals(optimal, subOptimal);
        assertEquals(5, Sequences.commonness(optimal));
        assertEquals(2, Sequences.commonness(subOptimal));

        // TODO "I have it" is split
        // TODO ~29073 candidates take long to compute
        // TODO It's ready could be matched -> candidate with less slices?
        // TODO "it's",4 could be joined if processing was delayed until after "yes"
        // -> "it's ready Miss" could be common
        // test after each disjunct removal if there are now common start elements
        // order of execution?
        assertEquals(9, optimal.size());
        assertEquals(new PhraseStringSequences(result("No", 0), result("It's", 4)), optimal.get(0));
        assertEquals(new PhraseStringSequences(result("Yes", 1, 3)), optimal.get(1));
        assertEquals(new PhraseStringSequences(result("It's", 3)), optimal.get(2));
        assertEquals(new PhraseStringSequences(result("ready", 3, 4)), optimal.get(3));
        assertEquals(new PhraseStringSequences(result("I", 2)), optimal.get(4));
        assertEquals(new PhraseStringSequences(result("have it", 2)), optimal.get(5));
        assertEquals(new PhraseStringSequences(result("Miss", 0, 1, 2, 3, 4)), optimal.get(6));
        assertEquals(new PhraseStringSequences(result("I'm", 0, 1)), optimal.get(7));
        assertEquals(new PhraseStringSequences(result("sorry", 0), result("ready", 1)), optimal.get(8));
    }

    private Integer max(Map<Integer, List<List<Sequences<PhraseString>>>> map) {
        return map.keySet().stream().reduce(Math::max).orElseThrow();
    }

}