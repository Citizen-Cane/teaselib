package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class PhrasesTest {

    @Test
    public void testPhrasesOfStrings()
            throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
        Phrases phrases = Phrases.of( //
                "Yes Miss, of course", //
                "No, of course not, Miss");

        assertEquals(3, phrases.size());

        assertEquals(new Phrases.Rule("Yes Miss", "No"), phrases.get(0));
        assertEquals(new Phrases.Rule("of course"), phrases.get(1));
        assertEquals(new Phrases.Rule("", "not Miss"), phrases.get(2));

        SRGSBuilder srgs = new SRGSBuilder(phrases);
        String xml = srgs.toXML();
        System.out.println(xml);
    }

    @Test
    public void testSliceAlternativePhrasesOfChoices()
            throws TransformerFactoryConfigurationError, ParserConfigurationException, TransformerException {
        String[] yes = { //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "of course" };
        String[] no = { //
                "No Miss, of course not", //
                "No, of course not, Miss", //
                "No, of course not", //
                "of course not" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", yes),
                new Choice("No #title, of course not", "No Miss, of course not", no));
        Phrases phrases = Phrases.of(choices);

        assertEquals(3, phrases.size());

        assertEquals(
                Phrases.rule(Phrases.oneOf("Yes Miss", "Yes", "Yes", ""), Phrases.oneOf("No Miss", "No", "No", "")),
                phrases.get(0));
        assertEquals(Phrases.rule(Phrases.oneOf("of course")), phrases.get(1));
        assertEquals(Phrases.rule(Phrases.oneOf("", "Miss", "", ""), Phrases.oneOf("not", "not Miss", "not", "not")),
                phrases.get(2));

        SRGSBuilder srgs = new SRGSBuilder(phrases);
        String xml = srgs.toXML();
        System.out.println(xml);
    }

}
