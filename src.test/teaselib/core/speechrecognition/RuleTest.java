package teaselib.core.speechrecognition;

import static java.lang.Integer.MIN_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High),
                // no "B C" - correct
                new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High),
                new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal),
                // neither "E F" nor "I" -> I expected
                new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High),
                new Rule("r_4_3_14", "K", 4, indices(3), 2, 4, 0.46f, Low)), 0, 6, 0.82f, High);

        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Rule name", "A D I K", rule.text);
        assertTrue("Indices in repaired rule not updated", !rule.indices.isEmpty());
    }

    @Test
    public void testSpeechDetectedRuleRepairSample2() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, // indices(3, 5),
                Arrays.asList( //
                        new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High),
                        // no "B C" - correct
                        new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High),
                        new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal),
                        // neither "E F" nor "I" -> I expected
                        new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High),
                        new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f, Normal),
                        new Rule("r_5_1,2,3,4_17", "B", 5, indices(1, 2, 3, 4), 4, 5, 0.80f, High)),
                0, 6, 0.82414407f, High);
        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Rule name", "A D I K L B", rule.text);
        assertTrue("Indices in repaired rule not updated", !rule.indices.isEmpty());
    }

    @Test
    public void testSpeechDetectedMultipleNullRulesRepair() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f, High),
                // no "B C" - correct
                new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f, High),
                new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f, Normal),
                // neither "E F" nor "I" -> I expected
                new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f, High),
                new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f, Normal),
                new Rule("r_5_1,2,3,4_17", null, 5, indices(0, 5), 4, 5, 1.0f, High)), 0, 6, 0.82414407f, High);

        assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        assertEquals("Legal NULL rule", null, rule.children.get(1).text);
        assertEquals("Repair", "I", rule.children.get(3).text);
        assertEquals("Repair", "B C", rule.children.get(5).text);
        assertEquals("Rule name", "A D I K L B C", rule.text);
        assertTrue("Indices in repaired rule not updated", !rule.indices.isEmpty());
    }

    @Test
    public void testTrailingNullRule() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("D E F, A B C", 0), //
                choice("G, A B C, H", 1));

        Rule speechDetected = new Rule("Main", "G, A B C", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_1_1", "G", 0, indices(1), 0, 1, 0.82f, High),
                new Rule("r_1_1_2", "A B C", 1, indices(1), 1, 4, 0.51f, Normal),
                new Rule("r_2_0_3", null, 2, indices(0), 4, 5, 1.0f, High)), 0, 6, 0.82414407f, High);
        assertEquals(Collections.singleton(1), speechDetected.intersectionWithoutNullRules());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        assertEquals("Rule name", "G A B C H", rule.text);
        assertTrue("Indices in repaired rule not updated", !rule.indices.isEmpty());
        assertEquals("Rule indices", Collections.singleton(1), rule.indices);

        assertEquals("Rule probability", rule.probability, rule.children.get(2).probability, 0.0001);
    }

    @Test
    public void testTrailingNullRuleFollowedBySpuriousExtraRule() {
        PhraseStringSequences choices = new PhraseStringSequences( //
                choice("D E F, A B C", 0), //
                choice("G, A B C, H", 1));

        Rule speechDetected = new Rule("Main", "G, A B C", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0_1", "G", 0, indices(1), 0, 1, 0.82f, High),
                new Rule("r_1_0_2", "A B C", 1, indices(1), 1, 4, 0.51f, Normal),
                new Rule("r_2_3", null, 2, indices(0), 4, 5, 1.0f, High),
                new Rule("", null, MIN_VALUE, indices(), 5, 5, 1.0f, High)), 0, 6, 0.82414407f, High);
        assertEquals(Collections.singleton(1), speechDetected.intersectionWithoutNullRules());

        assertTrue(speechDetected.hasTrailingNullRule());
        Rule speechDetectedwithoutTrailingNullRules = speechDetected.withoutIgnoreableTrailingNullRules();
        assertFalse(speechDetectedwithoutTrailingNullRules.hasTrailingNullRule());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetectedwithoutTrailingNullRules.repair(slicedPhrases);
        assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        assertEquals("Rule name", "G A B C H", rule.text);
        assertTrue("Indices in repaired rule not updated", !rule.indices.isEmpty());
        assertEquals("Rule indices", Collections.singleton(1), rule.indices);

        assertEquals("Rule probability", rule.probability, rule.children.get(2).probability, 0.0001);
    }

    @Test
    public void testirregularPhrases() {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        PhraseStringSequences choices = new PhraseStringSequences( //
                choice(sorry), choice(ready), choice(haveIt), choice(ready2), choice(ready3));

        Rule speechDetected = new Rule("Main", "it's ready", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_4", null, 0, indices(4), 0, 1, 1.0f, High),
                new Rule("r_1_3_4", "it's", 1, indices(3, 4), 1, 2, 0.51f, Normal),
                new Rule("r_2_1_3_4", "ready", 2, indices(1, 3, 4), 2, 3, 0.51f, Normal)), 0, 3, 0.82414407f, High);
        assertEquals(Collections.singleton(4), speechDetected.indices);
        assertEquals(new HashSet<>(Arrays.asList(3, 4)), speechDetected.intersectionWithoutNullRules());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        assertEquals(0, repaired.size());
    }

}
