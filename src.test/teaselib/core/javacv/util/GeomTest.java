/**
 * 
 */
package teaselib.core.javacv.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class GeomTest {
    @Test
    public void testCircularity() {
        // all list values are square values
        assertTrue(Geom.isCircular(Arrays.asList(1, 1, 1), 1.0));
        assertFalse(Geom.isCircular(Arrays.asList(1, 1, 1), 0.0));
        assertTrue(Geom.isCircular(Arrays.asList(4, 4, 4), 1.0));
        assertFalse(Geom.isCircular(Arrays.asList(4, 4, 9), 1.0));
        assertTrue(Geom.isCircular(Arrays.asList(4, 4, 9), 1.41));
        assertTrue(Geom.isCircular(Arrays.asList(4, 4, 16), 2.0));
        assertFalse(Geom.isCircular(Arrays.asList(4, 4, 16), 1.41));
    }
}
