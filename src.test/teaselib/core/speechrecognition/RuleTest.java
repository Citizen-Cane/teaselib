package teaselib.core.speechrecognition;

import static java.lang.Integer.MIN_VALUE;
import static org.junit.Assert.assertEquals;
import static teaselib.core.speechrecognition.Confidence.High;
import static teaselib.core.speechrecognition.Confidence.Low;
import static teaselib.core.speechrecognition.Confidence.Normal;
import static teaselib.core.speechrecognition.srgs.PhrasesSliceTest.choice;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.PhraseStringSequences;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;

public class RuleTest {

    private static Set<Integer> indices(Integer... indices) {
        return new HashSet<>(Arrays.asList(indices));
    }

    private static SlicedPhrases<PhraseString> testSliceMultipleCommon3() {
        // same as teaselib.core.speechrecognition.srgs.PhrasesSliceTest.testSliceMultipleCommon3()
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("A B C, D E F G", 0), //
                choice("D E F H, B C", 1), //
                choice("D I J, B C", 2), //
                choice("A, D I K L, B C", 3), //
                choice("D I F M, B C", 4), //
                choice("N B C, O", 5));
        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        assertEquals(6, slicedPhrases.size());
        return slicedPhrases;
    }

    @Test
    public void testSpeechDetectedRuleRepairSample1() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, indices(3, 5), 0, 6, 0.82f, High);
        speechDetected.children.add(new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High));
        // no "B C" - correct
        speechDetected.children.add(new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High));
        speechDetected.children.add(new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal));
        // neither "E F" nor "I" -> I expected
        speechDetected.children.add(new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High));
        speechDetected.children.add(new Rule("r_4_3_14", "K", 4, indices(3), 2, 4, 0.46f, Low));

        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Rule name", "A D I K", rule.text);
    }

    @Test
    public void testSpeechDetectedRuleRepairSample2() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, indices(3, 5), 0, 6, 0.82414407f, High);
        speechDetected.children.add(new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High));
        // no "B C" - correct
        speechDetected.children.add(new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High));
        speechDetected.children.add(new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal));
        // neither "E F" nor "I" -> I expected
        speechDetected.children.add(new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High));
        speechDetected.children.add(new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f, Normal));
        speechDetected.children.add(new Rule("r_5_1,2,3,4_17", "B", 5, indices(1, 2, 3, 4), 4, 5, 0.80f, High));

        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Rule name", "A D I K L B", rule.text);
    }

    @Test
    public void testSpeechDetectedMultipleNullRulesRepair() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L", MIN_VALUE, indices(3, 5), 0, 6, 0.82414407f, High);
        speechDetected.children.add(new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High));
        // no "B C" - correct
        speechDetected.children.add(new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High));
        speechDetected.children.add(new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal));
        // neither "E F" nor "I" -> I expected
        speechDetected.children.add(new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High));
        speechDetected.children.add(new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f, Normal));
        speechDetected.children.add(new Rule("r_5_1,2,3,4_17", null, 5, indices(0, 5), 4, 5, 0.63f, Normal));

        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Repair", "B C", rule.children.get(5).text);
        assertEquals("Rule name", "A D I K L B C", rule.text);
    }
}
