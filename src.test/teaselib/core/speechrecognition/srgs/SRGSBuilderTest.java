package teaselib.core.speechrecognition.srgs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import teaselib.core.ui.Choice;

public class SRGSBuilderTest {

    @Test
    public void verifyThatListSequenceToStringOutputsPlainWords() {
        StringSequence test = StringSequence.ignoreCase("The dog looked over the fence");
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

    static final Choice Optional = new Choice("");

    @Test
    public void testSRGSBuilderMultipleChoiceResults() throws ParserConfigurationException, TransformerException {
        String template = "A %0 %1, %2";
        Choice[] material = { new Choice("leather"), new Choice("rubber"), Optional };
        Choice[] dogToy = { new Choice("ball"), new Choice("bone"), new Choice("dildo") };
        Choice[] formOfAddress = { new Choice("#title", "Miss", "Miss", "Mistress", "dear Mistress") };
        Choice[][] args = { material, dogToy, formOfAddress };

        Phrases phrases = Phrases.of(Collections.emptyList());
        int groupIndex = 0;
        int ruleIndex = 0;
        for (String word : StringSequence.splitWords(template)) {
            if (word.startsWith("%")) {
                Choice[] arg = args[Integer.parseInt(word.substring(1))];
                List<OneOf> items = new ArrayList<>();
                Rule rule = new Rule(groupIndex, ruleIndex++, items);
                for (int choiceIndex = 0; choiceIndex < arg.length; choiceIndex++) {
                    rule.add(new OneOf(Collections.singletonList(choiceIndex), arg[choiceIndex].phrases));
                }
                phrases.add(rule);
            } else {
                phrases.add(Phrases.rule(groupIndex, ruleIndex++, word));
            }
        }

        assertEquals(4, phrases.size());
        assertEquals(Phrases.rule(0, 0, "A"), phrases.get(0));
        assertEquals(Phrases.rule(0, 1, "leather", "rubber", ""), phrases.get(1));
        assertEquals(Phrases.rule(0, 2, "ball", "bone", "dildo"), phrases.get(2));
        assertEquals(Phrases.rule(0, 3, Phrases.oneOf(0, "Miss", "Mistress", "dear Mistress")), phrases.get(3));

        assertTrue(phrases.get(0).get(0).contains("A"));
        assertTrue(phrases.get(1).get(0).contains("leather"));
        assertTrue(phrases.get(1).get(1).contains("rubber"));
        assertTrue(phrases.get(1).get(2).contains(""));
        assertTrue(phrases.get(2).get(0).contains("ball"));
        assertTrue(phrases.get(2).get(1).contains("bone"));
        assertTrue(phrases.get(2).get(2).contains("dildo"));

        assertTrue(phrases.get(3).get(0).contains("Miss"));
        assertTrue(phrases.get(3).get(0).contains("Mistress"));
        assertTrue(phrases.get(3).get(0).contains("dear Mistress"));

        // TODO Implement SRGS builder that handles multiple independent phrases
        SRGSBuilder srgs = new SRGSBuilder(phrases);
        String xml = srgs.toXML();
        assertFalse(xml.isEmpty());
        System.out.println(xml);

        // TODO Implement host input method that supports multiple phrases
        // assertRecognized(phrases, "A rubber ball Miss", new Prompt.Result(0));
        // assertRecognized(phrases, "A leather ball Miss", new Prompt.Result(1));
        // assertRecognized(phrases, "A leather bone Miss", new Prompt.Result(1));
        // assertRecognized(phrases, "A rubber bone Miss", new Prompt.Result(0));
        // assertRecognized(phrases, "A dildo Miss", new Prompt.Result(1));
        // assertRecognized(phrases, "A rubber dildo Miss", new Prompt.Result(1));
    }

}
