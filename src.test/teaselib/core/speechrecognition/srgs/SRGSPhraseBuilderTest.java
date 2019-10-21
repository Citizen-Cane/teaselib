package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertNotEquals;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class SRGSPhraseBuilderTest {

    @Test
    public void testCommonStart()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(new Choice("A, B"), new Choice("A, C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testCommonEnd()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(new Choice("A, C"), new Choice("B, C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

}
