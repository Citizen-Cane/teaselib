package teaselib;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.core.texttospeech.Voice;
import teaselib.util.TextVariables;

public class TextVariablesTests {

    private static final Actor actor = new Actor(Actor.Dominant,
            Voice.Gender.Female, "en-us");

    @Test
    public void testDefaultMatching() {
        String expected = "You're My slave.";
        String acual = TextVariables.Defaults.expand("You're My #"
                + TextVariables.Defaults.get(TextVariables.Names.Slave) + ".");
        assertEquals(expected, acual);
    }

    @Test
    public void testMatching() {
        assertEquals("You can call me Mistress Allison.",
                TextVariables.Defaults
                        .expand("You can call me #"
                                + TextVariables.Defaults
                                        .get(TextVariables.Names.Mistress)
                                + " Allison."));
    }

    @Test
    public void testMatchingMultipleStringsAtOnce() {
        List<String> expected = Arrays.asList("Yes, Miss", "No, Miss");
        List<String> actual = Arrays.asList("Yes, #miss", "No, #miss");
        assertEquals(expected, TextVariables.Defaults.expand(actual));
    }

    @Test
    public void testMatchingMultipleStringsAtOnceUpperLower() {
        List<String> expected = Arrays.asList("Yes, Miss", "No, Miss");
        List<String> actual = Arrays.asList("Yes, #mISs", "No, #MIss");
        assertEquals(expected, TextVariables.Defaults.expand(actual));
    }

}
