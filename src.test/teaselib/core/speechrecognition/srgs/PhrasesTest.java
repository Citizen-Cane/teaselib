package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.util.math.Partition;

public class PhrasesTest {

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfOfStringsTwoOptionalParts() {
        Phrases phrases = Phrases.of( //
                "Yes Miss, of course", //
                "Of course not, Miss");

        // TODO Resolve Index-Out-Of-Bounds when rules are joined -> remove this code from phrase processing

        // TODO Either only two rules with "of course" replicated
        // TODO or both choice with optional part
        // - either different groups or
        // - patched in SRGSBuilder as different rules
        // must tag with different groups but common part -> SRGSBuilder can detect situation
        // - cannot define common part for multiple groups
        //
        // -> SRGSBuilder must resolve OneOf

        // -> number of result choice indices may differ but all result values must be the same

        assertEquals(3, phrases.size());

        assertEquals(Phrases.rule(0, 0, "Yes Miss", ""), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "", "not Miss"), phrases.get(2));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStringsOneOptionalPart() {
        Phrases phrases = Phrases.of( //
                "Yes Miss, of course", //
                "No, of course not, Miss");

        // TODO Either only two rules with "of course" replicated
        // TODO or second choice with optional part -> Prompt.Result of different sizes
        // -> number of result choice indices may differ but all result values must be the same

        assertEquals(3, phrases.size());

        assertEquals(Phrases.rule(0, 0, "Yes Miss", "No"), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "", "not Miss"), phrases.get(2));
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStrings3() {
        Phrases phrases = Phrases.of( //
                "Yes, of course, Miss", //
                "No, of course not, Miss");

        assertEquals(4, phrases.size());

        // TODO Split into 2 choice parts -> could be one choice part but it's not joined
        // TODO or "of course" could be common -> "not" only in choice 1 -> Prompt.Result of different sizes

        assertEquals(Phrases.rule(0, 0, "Yes", "No"), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(Phrases.COMMON_RULE, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "", "not"), phrases.get(2));
        assertEquals(Phrases.rule(0, 3, Phrases.oneOf(Phrases.COMMON_RULE, "Miss")), phrases.get(3));
    }

    @Test
    public void testPhraseGrouping() {
        List<String> strings = Arrays.asList("Yes Miss", "Yes Mistress", "Of course Miss", "I have it");
        Partition<String> grooupedPhrases = new Partition<>(strings, SimplifiedPhrases::haveCommonParts);
        assertEquals(2, grooupedPhrases.groups.size());
    }

    @Test
    public void testFlattenString() {
        List<String> foobar = Arrays.asList("My name is Foo");
        Phrases phrases = Phrases.of(foobar);

        Sequences<String> flattened = phrases.flatten();
        assertEquals(1, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(1, restored.size());
        assertEquals("My name is Foo", restored.get(0));
    }

    @Test
    public void testFlattenStringsTogether() {
        List<String> foobar = Arrays.asList("My name is Foo", "My name is Bar", "My name is Foobar");
        Phrases phrases = Phrases.of(foobar);

        Sequences<String> flattened = phrases.flatten();
        assertEquals(3, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(3, restored.size());
        assertEquals("My name is Foo", restored.get(0));
        assertEquals("My name is Bar", restored.get(1));
        assertEquals("My name is Foobar", restored.get(2));
    }

    @Test
    public void testFlattenStringsSeparately() {
        List<String> strings = Arrays.asList("Yes Miss", "Yes Mistress", "Of course Miss", "I have it");
        Phrases phrases = Phrases.of(strings);

        Sequences<String> flattened = phrases.flatten();
        assertEquals(4, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(4, restored.size());
        assertEquals("Yes Miss", restored.get(0));
        assertEquals("Yes Mistress", restored.get(1));
        assertEquals("Of course Miss", restored.get(2));
        assertEquals("I have it", restored.get(3));
    }

    @Test
    public void testFlattenChoicesTogether() {
        Choices foobar = new Choices(Arrays.asList(new Choice("My name is Foo"), new Choice("My name is Bar"),
                new Choice("My name is Foobar")));
        Phrases phrases = Phrases.of(foobar);

        Sequences<String> flattened = phrases.flatten();
        assertEquals(3, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(3, restored.size());
        assertEquals("My name is Foo", restored.get(0));
        assertEquals("My name is Bar", restored.get(1));
        assertEquals("My name is Foobar", restored.get(2));
    }

    @Test
    public void testFlattenMultipleChoicesAlternativePhrases() {
        String[] yes = { //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "Of course", //
                "I have it" };
        String[] no = { //
                "No Miss, of course not", //
                "No, of course not, Miss", //
                "No, of course not", //
                "Of course not", //
                "I don't have it" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
        Phrases phrases = Phrases.of(choices);
        assertEquals(8, phrases.size());

        Sequences<String> flatten = phrases.flatten();
        List<String> flattened = flatten.stream().map(Sequence<String>::toString).collect(Collectors.toList());
        assertEquals(2, flattened.size());
    }

    @Test
    public void testFlattenChoicesSeparately() {
        Choices choices = new Choices(new Choice("Yes Miss"), new Choice("Yes Mistress"), new Choice("Of course Miss"),
                new Choice("I have it"));
        Phrases phrases = Phrases.of(choices);

        Sequences<String> flattened = phrases.flatten();
        assertEquals(4, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(4, restored.size());
        assertEquals("Yes Miss", restored.get(0));
        assertEquals("Yes Mistress", restored.get(1));
        assertEquals("Of course Miss", restored.get(2));
        assertEquals("I have it", restored.get(3));
    }

}
