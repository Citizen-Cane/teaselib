package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.core.texttospeech.Voice;
import teaselib.util.TextVariables;

public class TextVariablesTests {
    enum Names {
        First,
        Second,
        Third
    }

    private static TextVariables createTestData() {
        TextVariables testData = new TextVariables();
        testData.put(Names.First, "Peter");
        testData.put(Names.Second, "Paul");
        testData.put(Names.Third, "Mary");
        return testData;
    }

    @Test
    public void testDefaultMatching() {
        String expected = "You're My slave.";
        String acual = TextVariables.Defaults.expand("You're My #"
                + TextVariables.Defaults.get(TextVariables.Names.Slave) + ".");
        assertEquals(expected, acual);
    }

    @Test
    public void testMatching() {
        TextVariables testData = createTestData();
        assertEquals("My name is Peter.", testData
                .expand("My name is #" + testData.get(Names.First) + "."));
    }

    @Test
    public void testMatchingMultipleStringsAtOnce() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.",
                "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #first.",
                "Second name: #second.");
        assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testMatchingMultipleStringsAtOnceUpperLower() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.",
                "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #First.",
                "Second name: #sECond.");
        assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testNonExisting() {
        List<String> expected = Arrays.asList("Yes, AAaa", "No, aAAa");
        List<String> actual = Arrays.asList("Yes, #AAaa", "No, #aAAa");
        assertEquals(expected, TextVariables.Defaults.expand(actual));
    }

    @Test
    public void testActor() {
        Actor actor = new Actor("Miss Mary", "Ma'am", Voice.Gender.Female,
                "en-uk");
        List<String> expected = Arrays.asList(
                "Yes, " + actor.textVariables.get(Actor.FormOfAddress.Name),
                "No, " + actor.textVariables.get(Actor.FormOfAddress.FullName));
        List<String> actual = Arrays.asList("Yes, #name", "No, #FullName");
        assertEquals(expected, actor.textVariables.expand(actual));
    }
}
