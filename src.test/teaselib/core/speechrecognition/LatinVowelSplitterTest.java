package teaselib.core.speechrecognition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LatinVowelSplitterTest {

    @Test
    public void testSingleSplit() {
        String[] syllables = new LatinVowelSplitter(8).split("This is a test!");
        assertEquals(4, syllables.length);
    }

    @Test
    public void testMultipleSyllablesPerWord() {
        String[] syllables = new LatinVowelSplitter(8)
                .split("I approached the house by noon.");
        assertEquals(9, syllables.length);
    }

    @Test
    public void testMultipleSyllablesPerWord2() {
        String[] syllables = new LatinVowelSplitter(8)
                .split("There's a tree by the streetlight.");
        assertEquals(8, syllables.length);
    }
}
