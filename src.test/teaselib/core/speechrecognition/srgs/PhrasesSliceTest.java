package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Test;

public class PhrasesSliceTest {

    private static final Sequence<ChoiceString> none = new Sequence<>();

    static Sequence<ChoiceString> choice(String string, Integer... choices) {
        return new Sequence<>(new ChoiceString(string, Stream.of(choices).collect(toSet())).words(),
                ChoiceString::samePhrase);
    }

    static Sequence<ChoiceString> result(String string, Integer... choices) {
        return new Sequence<>(Collections.singletonList(new ChoiceString(string, Stream.of(choices).collect(toSet()))),
                ChoiceString::samePhrase);
    }

    @Test
    public void testCommonStart() {
        ChoiceStringSequences choices = new ChoiceStringSequences(choice("A, B", 0), choice("A, C", 1));

        Sequences<ChoiceString> common = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("A", 0, 1)), common);

        Sequences<ChoiceString> disjunct = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("B", 0), choice("C", 1)), disjunct);

        Sequences<ChoiceString> empty = choices.slice();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testCommonEnd() {
        ChoiceStringSequences choices = new ChoiceStringSequences(choice("A, C", 0), choice("B, C", 1));

        Sequences<ChoiceString> disjunct = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("A", 0), choice("B", 1)), disjunct);

        Sequences<ChoiceString> common = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("C", 0, 1)), common);

        Sequences<ChoiceString> empty = choices.slice();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testThatChoiceStringCaseIsIgnored() {
        ChoiceStringSequences choices = new ChoiceStringSequences(choice("yes Miss, of course", 0),
                choice("Yes, of Course, Miss", 1), choice("Of course", 2));

        Sequences<ChoiceString> slice1 = choices.slice();
        assertEquals(new ChoiceStringSequences(result("Yes", 0, 1)), slice1);

        Sequences<ChoiceString> slice2 = choices.slice();
        assertEquals(new ChoiceStringSequences(result("Miss", 0)), slice2);

        Sequences<ChoiceString> slice3 = choices.slice();
        assertEquals(new ChoiceStringSequences(result("of course", 0, 1, 2)), slice3);

        Sequences<ChoiceString> slice4 = choices.slice();
        assertEquals(new ChoiceStringSequences(result("Miss", 1)), slice4);

        Sequences<ChoiceString> empty = choices.slice();
        assertTrue(empty.isEmpty());

        // Sliced fine using separate indices
        // TODO -> map slice indices to choice indices when building phrase rules
    }

    // String[] no = { "No Miss, of course not", "No, of course not, Miss", "No, of course not", "Of course not" };

}
