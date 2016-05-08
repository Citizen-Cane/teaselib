/**
 * 
 */
package teaselib.core.javacv.util;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author someone
 *
 */
public class FramesPerSecondTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void test() {
        assertEquals(15.0, FramesPerSecond.getFps(30.0, 15.0), 0.0);
        assertEquals(10.0, FramesPerSecond.getFps(30.0, 10.0), 0.0);
        assertEquals(12.5, FramesPerSecond.getFps(25.0, 10.0), 0.0);
        assertEquals(12.0, FramesPerSecond.getFps(60.0, 12.5), 0.0);
        assertEquals(15.0, FramesPerSecond.getFps(30.0, 12.5), 0.0);
        // The tests below would fail, so be have to improve the function
        // assertEquals(12.5, FramesPerSecond.getFps(25.0, 15.0), 0.0);
    }

}
