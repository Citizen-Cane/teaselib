package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

public class SRGSBuilderTest {

    @Test
    public void testSRGSBuildFromListCommonEnd() throws ParserConfigurationException, TransformerException {
        List<List<String>> choices = new ArrayList<>();
        choices.add(Arrays.asList("Yes", "No"));
        choices.add(Arrays.asList("Miss"));
        SRGSBuilder<String> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void testSRGSBuildFromListCommonMiddle() throws ParserConfigurationException, TransformerException {
        List<List<String>> choices = new ArrayList<>();
        choices.add(Arrays.asList("Yes", "No"));
        choices.add(Arrays.asList("Miss"));
        choices.add(Arrays.asList("Of course", "I'm sorry"));
        SRGSBuilder<String> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void verifyThatListSequenceToStringOutputsPlainWords() {
        ListSequence<String> test = new ListSequence<>(ListSequenceUtil.splitWords("The dog looked over the fence"));
        assertFalse(test.toString().startsWith("["));
        assertFalse(test.toString().endsWith("]"));
    }

    @Test
    public void testSRGSBuildFromListSequence() throws ParserConfigurationException, TransformerException {
        List<ListSequences<String>> slices = ListSequenceUtil.slice( //
                "My dog jumped over the fence", //
                "The dog looked over the fence", //
                "he dog jumped over the fence", //
                "The dog jumped over the fence");
        SRGSBuilder<ListSequence<String>> srgs = new SRGSBuilder<>(slices);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void testSRGSBuilderMultipleChoices() throws ParserConfigurationException, TransformerException {
        String template = "A %0 %1, Miss";
        String[][] args = { { "leather", "rubber", "" }, { "ball", "bone", "Dildo" } };

        ListSequences<String> choices = new ListSequences<>();
        for (String word : ListSequenceUtil.splitWords(template)) {
            if (word.startsWith("%")) {
                String[] arg = args[Integer.parseInt(word.substring(1))];
                choices.add(new ListSequence<>(arg));
            } else {
                choices.add(new ListSequence<>(word));
            }
        }

        assertEquals(4, choices.size());
        assertEquals("A", choices.get(0).toString());
        assertEquals(new ListSequence<>(args[0]), choices.get(1));
        assertEquals(new ListSequence<>(args[1]), choices.get(2));
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

}
