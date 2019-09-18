package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.SpeechRecognitionTestUtils.flatten;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.util.math.Partition;

public class PhrasesIntegrationTest {

    private static final HashSet<Integer> CHOICES_0_1 = new HashSet<>(Arrays.asList(0, 1));

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStringsOneOptionalPart() {
        Phrases phrases = Phrases.of( //
                "Yes Miss, of course", //
                "No, of course not, Miss");

        assertEquals(Phrases.rule(0, 0, "Yes Miss", "No"), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "", "not Miss"), phrases.get(2));

        assertEquals(3, phrases.size());
    }

    @Test
    public void testSliceMultipleChoiceSinglePhraseOfStrings3() {
        Phrases phrases = Phrases.of( //
                "Yes, of course, Miss", //
                "No, of course not, Miss");

        assertEquals(Phrases.rule(0, 0, "Yes", "No"), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, Phrases.oneOf(CHOICES_0_1, "of course")), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "", "not"), phrases.get(2));
        assertEquals(Phrases.rule(0, 3, Phrases.oneOf(CHOICES_0_1, "Miss")), phrases.get(3));

        assertEquals(4, phrases.size());
    }

    @Test
    public void testPhraseGrouping() {
        List<PhraseString> strings = Arrays.asList(new PhraseString("Yes Miss", 0), new PhraseString("Yes Mistress", 0),
                new PhraseString("Of course Miss", 0), new PhraseString("I have it", 0));
        Partition<PhraseString> grooupedPhrases = new Partition<>(strings, Phrases::haveCommonParts);
        assertEquals(2, grooupedPhrases.groups.size());
    }

    @Test
    public void testFlattenString() {
        List<String> foobar = Arrays.asList("My name is Foo");
        Phrases phrases = Phrases.of(foobar);

        Sequences<String> flattened = flatten(phrases);
        assertEquals(1, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(1, restored.size());
        assertEquals("My name is Foo", restored.get(0));
    }

    @Test
    public void testFlattenStringsTogether() {
        List<String> foobar = Arrays.asList("My name is Foo", "My name is Bar", "My name is Foobar");
        Phrases phrases = Phrases.of(foobar);

        Sequences<String> flattened = flatten(phrases);
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

        Sequences<String> flattened = flatten(phrases);
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

        Sequences<String> flattened = flatten(phrases);
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
        assertEquals(3, phrases.size());

        Sequences<String> flatten = flatten(phrases);
        List<String> flattened = flatten.stream().map(Sequence<String>::toString).collect(Collectors.toList());
        assertEquals(2, flattened.size());
    }

    @Test
    public void testFlattenChoicesSeparately() {
        Choices choices = new Choices(new Choice("Yes Miss"), new Choice("Yes Mistress"), new Choice("Of course Miss"),
                new Choice("I have it"));
        Phrases phrases = Phrases.of(choices);
        assertEquals(4, phrases.choices());

        Sequences<String> flattened = flatten(phrases);
        assertEquals(4, flattened.size());

        List<String> restored = flattened.toStrings();
        assertEquals(4, restored.size());

        assertEquals("Yes Miss", restored.get(0));
        assertEquals("Yes Mistress", restored.get(1));
        assertEquals("Of course Miss", restored.get(2));
        assertEquals("I have it", restored.get(3));
    }

}
