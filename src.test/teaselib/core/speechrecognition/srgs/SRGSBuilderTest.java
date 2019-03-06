package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

public class SRGSBuilderTest {

    @Test
    public void testSRGSBuildFromList() throws ParserConfigurationException, TransformerException {
        List<List<String>> choices = new ArrayList<>();
        choices.add(Arrays.asList("Yes", "No"));
        choices.add(Arrays.asList("Miss"));
        SRGSBuilder<List<String>> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toString();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void testSRGSBuildFromList2() throws ParserConfigurationException, TransformerException {
        List<List<String>> choices = new ArrayList<>();
        choices.add(Arrays.asList("Yes", "No"));
        choices.add(Arrays.asList("Miss"));
        choices.add(Arrays.asList("Of course", "I'm sorry"));
        SRGSBuilder<List<String>> srgs = new SRGSBuilder<>(choices);
        String xml = srgs.toString();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

    @Test
    public void verifyThatListSequenceToStringOutputsPlainWords() throws ParserConfigurationException, TransformerException {
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
        String xml = srgs.toString();
        assertFalse(xml.isEmpty());
        System.out.println(xml);
    }

}
