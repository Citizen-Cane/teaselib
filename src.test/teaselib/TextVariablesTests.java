package teaselib;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.test.TestScript;
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
    public void testMatching() {
        TextVariables testData = createTestData();
        assertEquals("My name is Peter.", testData.expand("My name is #" + testData.get(Names.First) + "."));
    }

    @Test
    public void testMatchingMultipleStringsAtOnce() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.", "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #first.", "Second name: #second.");
        assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testMatchingMultipleStringsAtOnceUpperLower() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.", "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #First.", "Second name: #sECond.");
        assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testNonExisting() {
        List<String> expected = Arrays.asList("Yes, AAaa", "No, aAAa");
        List<String> actual = Arrays.asList("Yes, #AAaa", "No, #aAAa");
        assertEquals(expected, new TextVariables().expand(actual));
    }

    @Test
    public void testActor() {
        Actor actor = new Actor("Miss Mary", "Ma'am", Gender.Feminine, Locale.UK);
        List<String> expected = Arrays.asList("Yes, " + actor.textVariables.get(Actor.FormOfAddress.Name),
                "No, " + actor.textVariables.get(Actor.FormOfAddress.FullName));
        List<String> actual = Arrays.asList("Yes, #name", "No, #FullName");
        assertEquals(expected, actor.textVariables.expand(actual));
    }

    @Test
    public void testDefaultsMasculine() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Masculine));

        script.persistentEnum(Gender.class).set(Gender.Masculine);

        assertEquals("en", script.actor.locale().getLanguage());
        assertEquals("slave", script.expandTextVariables("#slave"));
        assertEquals("slave", script.expandTextVariables("#slave_title"));
        assertEquals("Slave", script.expandTextVariables("#slave_name"));
        assertEquals("Slave", script.expandTextVariables("#slave_fullname"));
    }

    @Test
    public void testDefaultsFeminine() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Masculine));

        script.persistentEnum(Gender.class).set(Gender.Feminine);

        assertEquals("en", script.actor.locale().getLanguage());
        assertEquals("slave-girl", script.expandTextVariables("#slave"));
        assertEquals("slave-girl", script.expandTextVariables("#slave_title"));
        assertEquals("Slave-girl", script.expandTextVariables("#slave_name"));
        assertEquals("Slave-girl", script.expandTextVariables("#slave_fullname"));
    }

    // TODO Test that covers TeaseLibConfigSetup, with writable identities user file, and host properties

}
