package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Test;

public class PhrasesSliceTest {

    static Sequence<PhraseString> choice(String string, Integer... choices) {
        return new Sequence<>(new PhraseString(string, Stream.of(choices).collect(toSet())).words(),
                PhraseString::samePhrase);
    }

    static Sequence<PhraseString> result(String string, Integer... choices) {
        return new Sequence<>(Collections.singletonList(new PhraseString(string, Stream.of(choices).collect(toSet()))),
                PhraseString::samePhrase);
    }

    @Test
    public void testCommonStart() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, B", 0), choice("A, C", 1));

        Sequences<PhraseString> common = choices.slice();
        assertEquals(new PhraseStringSequences(choice("A", 0, 1)), common);

        Sequences<PhraseString> disjunct = choices.slice();
        assertEquals(new PhraseStringSequences(choice("B", 0), choice("C", 1)), disjunct);

        Sequences<PhraseString> empty = choices.slice();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testCommonEnd() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("A, C", 0), choice("B, C", 1));

        Sequences<PhraseString> disjunct = choices.slice();
        assertEquals(new PhraseStringSequences(choice("A", 0), choice("B", 1)), disjunct);

        Sequences<PhraseString> common = choices.slice();
        assertEquals(new PhraseStringSequences(choice("C", 0, 1)), common);

        Sequences<PhraseString> empty = choices.slice();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testThatChoiceStringCaseIsIgnored() {
        PhraseStringSequences choices = new PhraseStringSequences(choice("yes Miss, of course", 0),
                choice("Yes, of Course, Miss", 1), choice("Of course", 2));

        Sequences<PhraseString> slice1 = choices.slice();
        assertEquals(new PhraseStringSequences(result("Yes", 0, 1)), slice1);

        Sequences<PhraseString> slice2 = choices.slice();
        assertEquals(new PhraseStringSequences(result("Miss", 0)), slice2);

        Sequences<PhraseString> slice3 = choices.slice();
        assertEquals(new PhraseStringSequences(result("of course", 0, 1, 2)), slice3);

        Sequences<PhraseString> slice4 = choices.slice();
        assertEquals(new PhraseStringSequences(result("Miss", 1)), slice4);

        Sequences<PhraseString> empty = choices.slice();
        assertTrue(empty.isEmpty());

        // Sliced fine using separate indices
        // TODO -> map slice indices to choice indices when building phrase rules
    }

    // String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not" };

}
