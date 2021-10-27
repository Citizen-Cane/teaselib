/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class RandomizedItemsTest {

    @Test
    public void testRandomized() throws IOException {
        try (TestScript script = new TestScript()) {
            assertNotNull(script.random.items(null, 1, null, 1));
            assertEquals(Collections.emptyList(), script.random.items(null, 1, null, 1));

            Integer[] introduction = { -2, 1 };
            Integer[] comments = { 0, 1, 2, 3 };
            assertEquals(2, script.random.items(introduction, 1, comments, 1).size());
            assertEquals(2, script.random.items(introduction, comments, 2).size());
            assertEquals(3, script.random.items(introduction, comments, 3).size());

            assertEquals(10, script.random.items(introduction, comments, 10).size());
            assertEquals(15, script.random.items(introduction, 5, comments, 10).size());
        }
    }

    @Test
    public void testRandomizedDegenerated() throws IOException {
        try (TestScript script = new TestScript()) {
            Integer[] introduction = { 1 };
            Integer[] comments = { 2 };
            assertEquals(1, script.random.items(introduction, 1, comments, 0).size());
            assertEquals(1, script.random.items(introduction, 0, comments, 1).size());
            assertEquals(2, script.random.items(introduction, comments, 2).size());
            assertEquals(3, script.random.items(introduction, comments, 3).size());

            assertEquals(10, script.random.items(introduction, comments, 10).size());
            assertEquals(15, script.random.items(introduction, 5, comments, 10).size());
        }
    }

}
