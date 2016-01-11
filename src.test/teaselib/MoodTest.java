/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

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
            assertTrue(mood.equalsIgnoreCase(Mood.Prefix + name + Mood.Suffix));
        }
    }
}
