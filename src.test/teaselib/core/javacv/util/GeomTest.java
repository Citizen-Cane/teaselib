/**
 * 
 */
package teaselib.core.javacv.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.junit.Test;

import teaselib.util.math.Partition;

public class GeomTest {
    @Test
    public void testCircularity() {
        // all list values are square values
        assertTrue(Geom.isCircular(Arrays.asList(1, 1, 1), 1.0));
        assertFalse(Geom.isCircular(Arrays.asList(1, 1, 1), 0.0));
        assertTrue(Geom.isCircular(Arrays.asList(4, 4, 4), 1.0));
        assertFalse(Geom.isCircular(Arrays.asList(4, 4, 9), 1.0));
        assertFalse(Geom.isCircular(Arrays.asList(4, 8), 1.33));
        assertTrue(Geom.isCircular(Arrays.asList(4, 8), 1.34));
        assertFalse(Geom.isCircular(Arrays.asList(4, 4, 16), 1.99));
        assertTrue(Geom.isCircular(Arrays.asList(4, 4, 16), 2.0));
    }

    @Test
    public void testPartition() throws Exception {
        try (Rect largestRect = new Rect(10, 10, 15, 10);
                Rect rect = new Rect(30, 30, 10, 5);
                Rect smallestRect = new Rect(20, 50, 5, 5);
                Rect anotherRect1 = new Rect(90, 90, 20, 15);
                Rect anotherRect2 = new Rect(100, 100, 15, 10);) {
            List<Rect> group1 = Arrays.asList(rect, largestRect, smallestRect);
            List<Rect> group2 = Arrays.asList(anotherRect1, anotherRect2);

            List<Rect> groups = Stream.concat(group1.stream(), group2.stream()).collect(Collectors.toList());
            List<Partition<Rect>.Group> partition = Geom.partition(groups, 25);

            assertEquals(2, partition.size());

            assertEquals(3, partition.get(0).items.size());
            assertEquals(Geom.join(group1).area(), partition.get(0).orderingElement.area());
            assertEquals(largestRect, partition.get(0).items.get(0));
            assertEquals(rect, partition.get(0).items.get(1));
            assertEquals(smallestRect, partition.get(0).items.get(2));

            assertEquals(2, partition.get(1).items.size());
            assertEquals(Geom.join(group2).area(), partition.get(1).orderingElement.area());
            assertEquals(anotherRect1, partition.get(1).items.get(0));
            assertEquals(anotherRect2, partition.get(1).items.get(1));
        }
    }
}
