package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.Sexuality.Gender;
import teaselib.test.TestScript;
import teaselib.util.TextVariables;
import teaselib.util.TextVariables.FormOfAddress;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
        Assertions.assertEquals("My name is Peter.", testData.expand("My name is #" + testData.get(Names.First) + "."));
    }

    @Test
    public void testMatchingMultipleStringsAtOnce() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.", "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #first.", "Second name: #second.");
        Assertions.assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testMatchingMultipleStringsAtOnceUpperLower() {
        TextVariables testData = createTestData();
        List<String> expected = Arrays.asList("First name: Peter.", "Second name: Paul.");
        List<String> actual = Arrays.asList("First name: #First.", "Second name: #sECond.");
        Assertions.assertEquals(expected, testData.expand(actual));
    }

    @Test
    public void testNonExisting() {
        List<String> expected = Arrays.asList("Yes, AAaa", "No, aAAa");
        List<String> actual = Arrays.asList("Yes, #AAaa", "No, #aAAa");
        Assertions.assertEquals(expected, new TextVariables().expand(actual));
    }

    @Test
    public void testActor() {
        Actor actor = new Actor("Miss Mary", "Ma'am", Gender.Feminine, Locale.UK);
        List<String> expected = Arrays.asList("Yes, " + actor.textVariables.get(FormOfAddress.Name),
                "No, " + actor.textVariables.get(FormOfAddress.FullName));
        List<String> actual = Arrays.asList("Yes, #name", "No, #FullName");
        Assertions.assertEquals(expected, actor.textVariables.expand(actual));
    }

    @Test
    public void testDefaultsMasculineEn() throws IOException {
        try (TestScript script = new TestScript(TestScript.newActor(Gender.Feminine, Locale.UK))) {
            script.persistence.newEnum(Gender.class).set(Gender.Masculine);

            Assertions.assertEquals("en", script.actor.locale().getLanguage());
            Assertions.assertEquals("slave", script.expandTextVariables("#slave"));
            Assertions.assertEquals("slave", script.expandTextVariables("#slave_title"));
            Assertions.assertEquals("slave", script.expandTextVariables("#slave_name"));
            Assertions.assertEquals("Slave", script.expandTextVariables("#slave_fullname"));
        }
    }

    @Test
    public void testDefaultsFeminineEn() throws IOException {
        try (TestScript script = new TestScript(TestScript.newActor(Gender.Masculine, Locale.UK))) {
            script.persistence.newEnum(Gender.class).set(Gender.Feminine);

            Assertions.assertEquals("en", script.actor.locale().getLanguage());
            Assertions.assertEquals("slave-girl", script.expandTextVariables("#slave"));
            Assertions.assertEquals("slave-girl", script.expandTextVariables("#slave_title"));
            Assertions.assertEquals("slave-girl", script.expandTextVariables("#slave_name"));
            Assertions.assertEquals("Slave-girl", script.expandTextVariables("#slave_fullname"));
        }
    }

    @Test
    public void testDefaultsMasculineDe() throws IOException {
        try (TestScript script = new TestScript(TestScript.newActor(Gender.Feminine, Locale.GERMAN))) {
            script.persistence.newEnum(Gender.class).set(Gender.Masculine);

            Assertions.assertEquals("de", script.actor.locale().getLanguage());
            Assertions.assertEquals("Sklave", script.expandTextVariables("#slave"));
            Assertions.assertEquals("Sklave", script.expandTextVariables("#slave_title"));
            Assertions.assertEquals("Sklave", script.expandTextVariables("#slave_name"));
            Assertions.assertEquals("Sklave", script.expandTextVariables("#slave_fullname"));
        }
    }

    @Test
    public void testDefaultsFemimineDe() throws IOException {
        try (TestScript script = new TestScript(TestScript.newActor(Gender.Masculine, Locale.GERMAN))) {
            script.persistence.newEnum(Gender.class).set(Gender.Feminine);

            Assertions.assertEquals("de", script.actor.locale().getLanguage());
            Assertions.assertEquals("Sklavin", script.expandTextVariables("#slave"));
            Assertions.assertEquals("Sklavin", script.expandTextVariables("#slave_title"));
            Assertions.assertEquals("Sklavin", script.expandTextVariables("#slave_name"));
            Assertions.assertEquals("Sklavin", script.expandTextVariables("#slave_fullname"));
        }
    }

    // TODO Test that covers TeaseLibConfigSetup, with writable identities user file, and host properties

}
