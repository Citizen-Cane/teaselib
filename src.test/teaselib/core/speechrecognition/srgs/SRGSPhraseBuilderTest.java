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

    @Test
    public void testCommonMiddle()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(new Choice("A C, D"), new Choice("B C, E"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testCommonEdges()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(new Choice("A B, D"), new Choice("A C, D"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testMultiplePhraseStartPositions()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        Choices choices = new Choices(new Choice("A B"), new Choice("B C"));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);
    }

    @Test
    public void testSingleChoiceMultiplePhrasesAreDistinct()
            throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        String[] yes = { "Yes Miss, of course", "Of course, Miss" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes));
        SRGSPhraseBuilder srgs = new SRGSPhraseBuilder(choices, "en_us");
        String xml = srgs.toXML();
        assertNotEquals("", xml);

        // TODO Yes Miss must be one-of together with special=NULL -> check index mapping
    }

}
