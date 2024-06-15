/**
 *
 */
package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.Collections;

/**
 * @author someone
 *
 */
public class RandomizedItemsTest {

    @Test
    public void testRandomized() throws IOException {
        try (TestScript script = new TestScript()) {
            Assertions.assertNotNull(script.random.items(null, 1, null, 1));
            Assertions.assertEquals(Collections.emptyList(), script.random.items(null, 1, null, 1));

            Integer[] introduction = {-2, 1};
            Integer[] comments = {0, 1, 2, 3};
            Assertions.assertEquals(2, script.random.items(introduction, 1, comments, 1).size());
            Assertions.assertEquals(2, script.random.items(introduction, comments, 2).size());
            Assertions.assertEquals(3, script.random.items(introduction, comments, 3).size());

            Assertions.assertEquals(10, script.random.items(introduction, comments, 10).size());
            Assertions.assertEquals(15, script.random.items(introduction, 5, comments, 10).size());
        }
    }

    @Test
    public void testRandomizedDegenerated() throws IOException {
        try (TestScript script = new TestScript()) {
            Integer[] introduction = {1};
            Integer[] comments = {2};
            Assertions.assertEquals(1, script.random.items(introduction, 1, comments, 0).size());
            Assertions.assertEquals(1, script.random.items(introduction, 0, comments, 1).size());
            Assertions.assertEquals(2, script.random.items(introduction, comments, 2).size());
            Assertions.assertEquals(3, script.random.items(introduction, comments, 3).size());

            Assertions.assertEquals(10, script.random.items(introduction, comments, 10).size());
            Assertions.assertEquals(15, script.random.items(introduction, 5, comments, 10).size());
        }
    }

}
