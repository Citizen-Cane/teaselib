package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;

public class PhrasesSliceTest {

    static Sequence<ChoiceString> choice(String string, Integer... choices) {
        return new Sequence<>(new ChoiceString(string, Stream.of(choices).collect(toSet())).words(),
                ChoiceString::samePhrase);
    }

    @Test
    public void testCommonStart() {
        ChoiceStringSequences choices = new ChoiceStringSequences(choice("A, B", 0), choice("A, C", 1));

        Sequences<ChoiceString> common = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("A", 0, 1)), common);

        Sequences<ChoiceString> disjunct = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("B", 0), choice("C", 1)), disjunct);

        assertTrue(true);
    }

    @Test
    public void testCommonEnd() {
        ChoiceStringSequences choices = new ChoiceStringSequences(choice("A, C", 0), choice("B, C", 1));

        Sequences<ChoiceString> disjunct = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("A", 0), choice("B", 1)), disjunct);

        Sequences<ChoiceString> common = choices.slice();
        assertEquals(new ChoiceStringSequences(choice("C", 0, 1)), common);

        assertTrue(true);
    }

}
