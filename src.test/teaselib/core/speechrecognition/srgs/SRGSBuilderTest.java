package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class SRGSBuilderTest {

    @Test
    public void verifyThatListSequenceToStringOutputsPlainWords() {
        Sequence<String> test = new Sequence<>(SequenceUtil.splitWords("The dog looked over the fence"));
        assertFalse(test.toString().startsWith("["));
        assertFalse(test.toString().endsWith("]"));
    }

    @Test
    public void testSRGSBuildFromListSequence() throws ParserConfigurationException, TransformerException {
        Phrases phrases = Phrases.of( //
                "My dog jumped over the fence", //
                "The dog looked over the fence", //
                "he dog jumped over the fence", //
                "The dog jumped over the fence");
        SRGSBuilder srgs = new SRGSBuilder(phrases);
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
        SRGSBuilder srgs = new SRGSBuilder(Phrases
                .of(new Choices(choices.stream().map(seq -> new Choice(seq.toString())).collect(Collectors.toList()))));
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
        Choice[] formOfAddress = { new Choice("#title", "Miss", "Miss", "Mistress", "dear Mistress") };
        Choice[][] args = { material, dogToy, formOfAddress };

        // TODO Choice is not for building phrases since it cannot be converted into a sequence
        Sequences<Choice> choices = new Sequences<>();
        for (String word : SequenceUtil.splitWords(template)) {
            if (word.startsWith("%")) {
                Choice[] arg = args[Integer.parseInt(word.substring(1))];
                choices.add(new Sequence<>(arg, Choice::getDisplay));
            } else {
                choices.add(new Sequence<>(Arrays.asList(new Choice(word)), Choice::getDisplay));
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

        // TODO Implement SRGS builder that handles multiple independent choices
        // SRGSBuilder srgs = new SRGSBuilder(choices);
        // String xml = srgs.toXML();
        // assertFalse(xml.isEmpty());
        // System.out.println(xml);

        // TODO Implement host input method that supports multiple choices
        // assertRecognized(choices, "A rubber ball Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A leather ball Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A leather bone Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber bone Miss", new Prompt.Result(0));
        // assertRecognized(choices, "A dildo Miss", new Prompt.Result(1));
        // assertRecognized(choices, "A rubber dildo Miss", new Prompt.Result(1));
    }

}
