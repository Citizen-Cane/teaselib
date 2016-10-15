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
