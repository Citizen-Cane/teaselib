/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.Test;

import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class RandomizedItemsTest {

    @Test
    public void testRandomized() {
        TestScript script = TestScript.getOne();

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
