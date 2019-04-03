package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.Test;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class PhrasesTest {

    @Test
    public void testSliceMultiplePhrases()
            throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
        List<List<Sequence<String>>> phrases = Phrases.of( //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "of course");

        assertEquals(3, phrases.size());

        assertEquals(new Sequences<>(new Sequence<>("Yes", "Miss"), new Sequence<>("Yes"), new Sequence<>("Yes"),
                new Sequence<>()), phrases.get(0));
        assertEquals(new Sequences<>(new Sequence<>("of", "course")), phrases.get(1));
        assertEquals(new Sequences<>(new Sequence<>(), new Sequence<>("Miss"), new Sequence<>(), new Sequence<>()),
                phrases.get(2));

        SRGSBuilder<Sequence<String>> srgs = new SRGSBuilder<>(phrases);
        String xml = srgs.toXML();
        System.out.println(xml);
    }

    @Test
    public void testSliceMultiplePhrasesFomChoices()
            throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
        String[] alternatives = { //
                "Yes Miss, of course", //
                "Yes, of course, Miss", //
                "Yes, of course", //
                "of course" };
        Choices choices = new Choices(new Choice("Yes #title, of course", "Yes Miss, of course", alternatives));
        Phrases phrases = Phrases.of(choices);

        // assertEquals(3, phrases.size());
        //
        // assertEquals(new Sequences<>(new Sequence<>("Yes", "Miss"), new Sequence<>("Yes"), new Sequence<>("Yes"),
        // new Sequence<>()), phrases.get(0));
        // assertEquals(new Sequences<>(new Sequence<>("of", "course")), phrases.get(1));
        // assertEquals(new Sequences<>(new Sequence<>(), new Sequence<>("Miss"), new Sequence<>(), new Sequence<>()),
        // phrases.get(2));
        //
        // SRGSBuilder<Sequence<String>> srgs = new SRGSBuilder<>(phrases);
        // String xml = srgs.toXML();
        // System.out.println(xml);
    }

}
