package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LatinVowelSplitterTest {

    @Test
    public void testSingleSplit() {
        String[] syllables = new LatinVowelSplitter(8).split("This is a test!");
        assertEquals(4, syllables.length);
    }

    @Test
    public void testMultipleVowelsPerWord() {
        String[] syllables = new LatinVowelSplitter(8)
                .split("I approached the house by noon.");
        assertEquals(9, syllables.length);
    }

    @Test
    public void testMultipleVowelsPerWord2() {
        String[] syllables = new LatinVowelSplitter(8)
                .split("There's a tree by the streetlight.");
        assertEquals(8, syllables.length);
    }

    @Test
    public void testMultipleChoices() {
        int minimumVowelsForHypothesisREcognition = 8;
        PromptSplitter latinVowelSplitter = new LatinVowelSplitter(
                minimumVowelsForHypothesisREcognition);
        assertEquals(minimumVowelsForHypothesisREcognition,
                latinVowelSplitter.getMinimumForHypothesisRecognition());

        List<String> promptsWithDifferentStart = Arrays.asList(
                "I'm waiting at the bar.",
                "You're waiting in front of the house.");
        assertEquals(minimumVowelsForHypothesisREcognition, latinVowelSplitter
                .getMinimumForHypothesisRecognition(promptsWithDifferentStart));

        List<String> promptsWithCommonStart = Arrays.asList(
                "I'm waiting at the bar.",
                "I'm waiting in front of the house.");
        assertEquals(3 + minimumVowelsForHypothesisREcognition, latinVowelSplitter
                .getMinimumForHypothesisRecognition(promptsWithCommonStart));
    }
}
