package teaselib.core.speechrecognition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.PhraseStringSymbols;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;

import java.util.*;

import static java.lang.Integer.MIN_VALUE;

public class RuleTest {

    private static Set<Integer> indices(Integer... indices) {
        return new HashSet<>(Arrays.asList(indices));
    }

    private static SlicedPhrases<PhraseString> testSliceMultipleCommon3() {
        // same as teaselib.core.speechrecognition.srgs.PhrasesSliceTest.testSliceMultipleCommon3()
        var choices = new PhraseStringSymbols(Arrays.asList(//
                "A B C, D E F G", //
                "D E F H, B C", //
                "D I J, B C", //
                "A, D I K L, B C", //
                "D I F M, B C", //
                "N B C, O"));
        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        Assertions.assertEquals(6, slicedPhrases.size());
        return slicedPhrases;
    }

    @Test
    public void testSpeechDetectedRuleRepairSample1() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f),
                // no "B C" - correct
                new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f),
                new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f),
                // neither "E F" nor "I" -> I expected
                new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f),
                new Rule("r_4_3_14", "K", 4, indices(3), 2, 4, 0.46f)), 0, 6, 0.82f);

        Assertions.assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        Assertions.assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        Assertions.assertNull(rule.children.get(1).text, "Legal NULL rule");
        Assertions.assertEquals("I", rule.children.get(3).text, "Repair");
        Assertions.assertEquals("A D I K", rule.text, "Rule name");
        Assertions.assertFalse(rule.indices.isEmpty(), "Indices in repaired rule not updated");
    }

    @Test
    public void testSpeechDetectedRuleRepairSample2() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L B", MIN_VALUE, // indices(3, 5),
                Arrays.asList( //
                        new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f),
                        // no "B C" - correct
                        new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f),
                        new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f),
                        // neither "E F" nor "I" -> I expected
                        new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f),
                        new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f),
                        new Rule("r_5_1,2,3,4_17", "B", 5, indices(1, 2, 3, 4), 4, 5, 0.80f)),
                0, 6, 0.82414407f);
        Assertions.assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        Assertions.assertEquals(1, repaired.size());
        Rule rule = repaired.get(0);
        Assertions.assertNull(rule.children.get(1).text, "Legal NULL rule");
        Assertions.assertEquals("I", rule.children.get(3).text, "Repair");
        Assertions.assertEquals("A D I K L B", rule.text, "Rule name");
        Assertions.assertFalse(rule.indices.isEmpty(), "Indices in repaired rule not updated");
    }

    @Test
    public void testSpeechDetectedMultipleNullRulesRepair() {
        SlicedPhrases<PhraseString> slicedPhrases = testSliceMultipleCommon3();

        Rule speechDetected = new Rule("Main", "A D K L", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0,3_1", "A", 0, indices(0, 3), 0, 1, 0.82f),
                // no "B C" - correct
                new Rule("r_1_1,2,3,4_5", null, 1, indices(1, 2, 3, 4), 1, 1, 1.0f),
                new Rule("r_2_0,1,2,3,4_7", "D", 2, indices(0, 1, 2, 3, 4), 1, 2, 0.62f),
                // neither "E F" nor "I" -> I expected
                new Rule("r_3_5_10", null, 3, indices(5), 2, 2, 1.0f),
                new Rule("r_4_3_14", "K L", 4, indices(3), 2, 4, 0.51f),
                new Rule("r_5_1,2,3,4_17", null, 5, indices(0, 5), 4, 5, 1.0f)), 0, 6, 0.82414407f);

        Assertions.assertEquals(Collections.singleton(3), speechDetected.intersectionWithoutNullRules());

        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        Assertions.assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        Assertions.assertNull(rule.children.get(1).text, "Legal NULL rule");
        Assertions.assertEquals("I", rule.children.get(3).text, "Repair");
        Assertions.assertEquals("B C", rule.children.get(5).text, "Repair");
        Assertions.assertEquals("A D I K L B C", rule.text, "Rule name");
        Assertions.assertFalse(rule.indices.isEmpty(), "Indices in repaired rule not updated");
    }

    @Test
    public void testTrailingNullRule() {
        var choices = new PhraseStringSymbols(Arrays.asList(//
                "D E F, A B C", //
                "G, A B C, H"));

        Rule speechDetected = new Rule("Main", "G, A B C", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_1_1", "G", 0, indices(1), 0, 1, 0.82f),
                new Rule("r_1_1_2", "A B C", 1, indices(1), 1, 4, 0.51f),
                new Rule("r_2_0_3", null, 2, indices(0), 4, 5, 1.0f)), 0, 6, 0.82414407f);
        Assertions.assertEquals(Collections.singleton(1), speechDetected.intersectionWithoutNullRules());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        Assertions.assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        Assertions.assertEquals("G A B C H", rule.text, "Rule name");
        Assertions.assertFalse(rule.indices.isEmpty(), "Indices in repaired rule not updated");
        Assertions.assertEquals(Collections.singleton(1), rule.indices, "Rule indices");

        Assertions.assertEquals(rule.probability, rule.children.get(2).probability, 0.0001, "Rule probability");
    }

    @Test
    public void testTrailingNullRuleFollowedBySpuriousExtraRule() {
        var choices = new PhraseStringSymbols(Arrays.asList(//
                "D E F, A B C", //
                "G, A B C, H"));

        Rule speechDetected = new Rule("Main", "G, A B C", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_0_1", "G", 0, indices(1), 0, 1, 0.82f),
                new Rule("r_1_0_2", "A B C", 1, indices(1), 1, 4, 0.51f),
                new Rule("r_2_3", null, 2, indices(0), 4, 5, 1.0f),
                new Rule("", null, MIN_VALUE, indices(), 5, 5, 1.0f)), 0, 6, 0.82414407f);
        Assertions.assertEquals(Collections.singleton(1), speechDetected.intersectionWithoutNullRules());

        Assertions.assertTrue(speechDetected.hasTrailingNullRule());
        Assertions.assertTrue(speechDetected.hasIgnoreableTrailingNullRule());
        Rule speechDetectedwithoutTrailingNullRules = speechDetected.withoutIgnoreableTrailingNullRules();
        Assertions.assertTrue(speechDetected.hasTrailingNullRule());
        Assertions.assertFalse(speechDetectedwithoutTrailingNullRules.hasIgnoreableTrailingNullRule());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetectedwithoutTrailingNullRules.repair(slicedPhrases);
        Assertions.assertEquals(1, repaired.size());

        Rule rule = repaired.get(0);
        Assertions.assertEquals("G A B C H", rule.text, "Rule name");
        Assertions.assertFalse(rule.indices.isEmpty(), "Indices in repaired rule not updated");
        Assertions.assertEquals(Collections.singleton(1), rule.indices, "Rule indices");

        Assertions.assertEquals(rule.probability, rule.children.get(2).probability, 0.0001, "Rule probability");
    }

    @Test
    public void testirregularPhrases() {
        String sorry = "No Miss, I'm sorry";
        String ready = "Yes Miss, I'm ready";
        String haveIt = "I have it, Miss";
        String ready2 = "Yes,it's ready, Miss";
        String ready3 = "It's ready, Miss";

        var choices = new PhraseStringSymbols(Arrays.asList(sorry, ready, haveIt, ready2, ready3));

        Rule speechDetected = new Rule("Main", "it's ready", MIN_VALUE, Arrays.asList( //
                new Rule("r_0_4", null, 0, indices(4), 0, 1, 1.0f),
                new Rule("r_1_3_4", "it's", 1, indices(3, 4), 1, 2, 0.51f),
                new Rule("r_2_1_3_4", "ready", 2, indices(1, 3, 4), 2, 3, 0.51f)), 0, 3, 0.82414407f);
        Assertions.assertEquals(Collections.singleton(4), speechDetected.indices);
        Assertions.assertEquals(new HashSet<>(Arrays.asList(3, 4)), speechDetected.intersectionWithoutNullRules());

        SlicedPhrases<PhraseString> slicedPhrases = SlicedPhrases.of(choices);
        List<Rule> repaired = speechDetected.repair(slicedPhrases);
        Assertions.assertEquals(0, repaired.size());
    }

}
