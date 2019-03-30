package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import teaselib.core.ui.Choice;

public class SRGSBuilderTest {

    @Test
    public void testSRGSBuildFromListCommonEnd() throws ParserConfigurationException, TransformerException {
        Sequences<String> choices = new Sequences<>();
        choices.add(new Sequence<>("Yes", "No"));
        choices.add(new Sequence<>("Miss"));
        SRGSBuilder<String> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void testSRGSBuildFromListCommonMiddle() throws ParserConfigurationException, TransformerException {
        Sequences<String> choices = new Sequences<>();
        choices.add(new Sequence<>("Yes", "No"));
        choices.add(new Sequence<>("Miss"));
        choices.add(new Sequence<>("Of course", "I'm sorry"));
        SRGSBuilder<String> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void verifyThatListSequenceToStringOutputsPlainWords() {
        Sequence<String> test = new Sequence<>(SequenceUtil.splitWords("The dog looked over the fence"));
        assertFalse(test.toString().startsWith("["));
        assertFalse(test.toString().endsWith("]"));
    }

    @Test
    public void testSlice() {
        List<Sequences<String>> slices = SequenceUtil.slice( //
                "My dog jumped over the fence", //
                "The dog looked over the fence", //
                "The dog jumped over the fence", //
                "The dog jumped over the fence");
        System.out.println(slices);

        // TODO slice returns list of sequences, each containing a Sequence of of string
        // -> Sequence<String> is the element type

        // TODO in testSRGSBuilderMultipleChoiceResults the element type T is Choice
        // - but Sequences requires it to be Sequence<T>
        //

        // SRGSBuilder needs:
        // + phrase
        // + phrase parts (choices or rule)
        // choice display elements <one-of>
        // -> List<List<Choice>>, and choice display is kind of a list as well
    }

    @Test
    public void testSRGSBuildFromListSequence() throws ParserConfigurationException, TransformerException {
        Phrases phrases = Phrases.of( //
                "My dog jumped over the fence", //
                "The dog looked over the fence", //
                "he dog jumped over the fence", //
                "The dog jumped over the fence");
        SRGSBuilder<Sequence<String>> srgs = new SRGSBuilder<>(phrases);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void testSRGSBuilderMultipleStringResults() throws ParserConfigurationException, TransformerException {
        String template = "A %0 %1, Miss";
        String[][] args = { { "leather", "rubber", "" }, { "ball", "bone", "dildo" } };

        Sequences<String> choices = new Sequences<>();
        for (String word : SequenceUtil.splitWords(template)) {
            if (word.startsWith("%")) {
                String[] arg = args[Integer.parseInt(word.substring(1))];
                choices.add(new Sequence<>(arg));
            } else {
                choices.add(new Sequence<>(word));
            }
        }

        assertEquals(4, choices.size());
        assertEquals("A", choices.get(0).toString());
        assertEquals(new Sequence<>(args[0]), choices.get(1));
        assertEquals(new Sequence<>(args[1]), choices.get(2));
        assertEquals("Miss", choices.get(3).toString());

        // TODO build builds same choice, must build any
        SRGSBuilder<String> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);

        // assertRecognized(choices, "A rubber ball Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A leather ball Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A leather bone Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber bone Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A dildo Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber dildo Miss", new Prompt.Result(1));
    }

    static final Choice Optional = new Choice("");

    @Test
    public void testSRGSBuilderMultipleChoiceResults() throws ParserConfigurationException, TransformerException {
        String template = "A %0 %1, %2";
        Choice[] material = { new Choice("leather"), new Choice("rubber"), Optional };
        Choice[] dogToy = { new Choice("ball"), new Choice("bone"), new Choice("dildo") };
        Choice[] formOfAddress = { new Choice("#title", "Miss", "Mistress", "dear Mistress") };
        Choice[][] args = { material, dogToy, formOfAddress };

        Sequences<Choice> choices = new Sequences<>();
        for (String word : SequenceUtil.splitWords(template)) {
            if (word.startsWith("%")) {
                Choice[] arg = args[Integer.parseInt(word.substring(1))];
                choices.add(new Sequence<>(arg, Choice::Display));
            } else {
                // TODO production code would expand text variables
                choices.add(new Sequence<>(Arrays.asList(new Choice(word)), Choice::Display));
            }
        }

        assertEquals(4, choices.size());
        assertEquals("A", choices.get(0).toString());
        assertEquals(new Sequence<>(args[0]), choices.get(1));
        assertEquals(new Sequence<>(args[1]), choices.get(2));
        assertEquals("Miss", choices.get(3).toString());

        assertEquals("A", choices.get(0).get(0).text);
        assertEquals("leather", choices.get(1).get(0).text);
        assertEquals("rubber", choices.get(1).get(1).text);
        assertEquals("", choices.get(1).get(2).text);
        assertEquals("ball", choices.get(2).get(0).text);
        assertEquals("bone", choices.get(2).get(1).text);
        assertEquals("dildo", choices.get(2).get(2).text);
        assertEquals("#title", choices.get(3).get(0).text);

        // TODO build builds same choice, must build any
        SRGSBuilder<Choice> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);

        // TODO This would work only if the host input methods supports multiple choices

        // assertRecognized(choices, "A rubber ball Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A leather ball Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A leather bone Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber bone Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A dildo Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber dildo Miss", new Prompt.Result(1));
    }

}
