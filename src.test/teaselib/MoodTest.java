/**
 *
 */
package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author someone
 *
 */
public class MoodTest {

    @Test
    public void testNaming() {
        for (String mood : Arrays.asList(Mood.Amused, Mood.Angry,
                Mood.Disappointed, Mood.Friendly, Mood.Harsh, Mood.Neutral,
                Mood.Pleased, Mood.Reading, Mood.Sceptic, Mood.Sorry)) {
            String name = Mood.extractName(mood);
            Assertions.assertTrue(mood.equalsIgnoreCase(Mood.Prefix + name + Mood.Suffix));
        }
    }
}
