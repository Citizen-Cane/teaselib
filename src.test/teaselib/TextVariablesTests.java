package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import teaselib.Sexuality.Gender;
import teaselib.test.TestScript;
import teaselib.util.TextVariables;
import teaselib.util.TextVariables.FormOfAddress;

public class TextVariablesTests {
    enum Names {
        First,
        Second,
        Third
    }

    private static TextVariables createTestData() {
        TextVariables testData = new TextVariables();
        testData.set(Names.First, "Peter");
        testData.set(Names.Second, "Paul");
        testData.set(Names.Third, "Mary");
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
        List<String> expected = Arrays.asList("Yes, " + actor.textVariables.get(FormOfAddress.Name),
                "No, " + actor.textVariables.get(FormOfAddress.FullName));
        List<String> actual = Arrays.asList("Yes, #name", "No, #FullName");
        assertEquals(expected, actor.textVariables.expand(actual));
    }

    @Test
    public void testDefaultsMasculineEn() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Feminine, Locale.UK));

        script.persistentEnum(Gender.class).set(Gender.Masculine);

        assertEquals("en", script.actor.locale().getLanguage());
        assertEquals("slave", script.expandTextVariables("#slave"));
        assertEquals("slave", script.expandTextVariables("#slave_title"));
        assertEquals("slave", script.expandTextVariables("#slave_name"));
        assertEquals("Slave", script.expandTextVariables("#slave_fullname"));
    }

    @Test
    public void testDefaultsFeminineEn() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Masculine, Locale.UK));

        script.persistentEnum(Gender.class).set(Gender.Feminine);

        assertEquals("en", script.actor.locale().getLanguage());
        assertEquals("slave-girl", script.expandTextVariables("#slave"));
        assertEquals("slave-girl", script.expandTextVariables("#slave_title"));
        assertEquals("slave-girl", script.expandTextVariables("#slave_name"));
        assertEquals("Slave-girl", script.expandTextVariables("#slave_fullname"));
    }

    @Test
    public void testDefaultsMasculineDe() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Feminine, Locale.GERMAN));

        script.persistentEnum(Gender.class).set(Gender.Masculine);

        assertEquals("de", script.actor.locale().getLanguage());
        assertEquals("Sklave", script.expandTextVariables("#slave"));
        assertEquals("Sklave", script.expandTextVariables("#slave_title"));
        assertEquals("Sklave", script.expandTextVariables("#slave_name"));
        assertEquals("Sklave", script.expandTextVariables("#slave_fullname"));
    }

    @Test
    public void testDefaultsFemimineDe() {
        TestScript script = TestScript.getOne(TestScript.newActor(Gender.Masculine, Locale.GERMAN));

        script.persistentEnum(Gender.class).set(Gender.Feminine);

        assertEquals("de", script.actor.locale().getLanguage());
        assertEquals("Sklavin", script.expandTextVariables("#slave"));
        assertEquals("Sklavin", script.expandTextVariables("#slave_title"));
        assertEquals("Sklavin", script.expandTextVariables("#slave_name"));
        assertEquals("Sklavin", script.expandTextVariables("#slave_fullname"));
    }

    // TODO Test that covers TeaseLibConfigSetup, with writable identities user file, and host properties

}
