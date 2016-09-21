/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class RandomizedItemsTest {
    final TeaseLib teaseLib = new TeaseLib(new DummyHost(),
            new DummyPersistence());
    final String namespace = "JUnit test";
    final TeaseScript script = new TeaseScript(teaseLib,
            new ResourceLoader(RandomizedItemsTest.class),
            new Actor("Test", Voice.Gender.Female, Locale.US), namespace) {
        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void testRandomized() {
        assertNotNull(script.randomized(null, 1, null, 1));
        assertEquals(Collections.EMPTY_LIST,
                script.randomized(null, 1, null, 1));

        Integer[] introduction = { -2, 1 };
        Integer[] comments = { 0, 1, 2, 3 };
        assertEquals(2, script.randomized(introduction, 1, comments, 1).size());
        assertEquals(2, script.randomized(introduction, comments, 2).size());
        assertEquals(3, script.randomized(introduction, comments, 3).size());

        assertEquals(10, script.randomized(introduction, comments, 10).size());
        assertEquals(15,
                script.randomized(introduction, 5, comments, 10).size());
    }

}
