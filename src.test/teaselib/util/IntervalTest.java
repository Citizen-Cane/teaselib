package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class IntervalTest {

    @Test
    public void testInterval() {
        assertThrows(IllegalArgumentException.class, () -> new Interval(4, 1));
    }

    @Test
    public void testIntersects() throws Exception {
        assertTrue(new Interval(1, 4).intersects(new Interval(3, 6)));
        assertTrue(new Interval(1, 4).intersects(new Interval(4, 6)));
        assertFalse(new Interval(1, 3).intersects(new Interval(4, 6)));

        assertTrue(new Interval(3, 6).intersects(new Interval(1, 4)));
        assertTrue(new Interval(4, 6).intersects(new Interval(1, 4)));
        assertFalse(new Interval(4, 6).intersects(new Interval(1, 3)));
    }

    @Test
    public void testContainsInterval() throws Exception {
        assertTrue(new Interval(1, 6).contains(new Interval(1, 6)));
        assertTrue(new Interval(1, 6).contains(new Interval(2, 4)));
        assertFalse(new Interval(1, 6).contains(new Interval(0, 4)));
        assertFalse(new Interval(1, 6).contains(new Interval(2, 7)));
        assertFalse(new Interval(1, 6).contains(new Interval(0, 7)));
    }

    @Test
    public void testContainsInt() throws Exception {
        Interval range = new Interval(1, 10);
        assertEquals(1, range.start);
        assertEquals(10, range.end);

        assertTrue(range.contains(1));
        assertTrue(range.contains(10));
        assertFalse(range.contains(0));
        assertFalse(range.contains(11));

        assertTrue(range.contains(Integer.valueOf(1)));
        assertTrue(range.contains(Integer.valueOf(10)));
        assertFalse(range.contains(Integer.valueOf(0)));
        assertFalse(range.contains(Integer.valueOf(11)));

        assertTrue(range.contains(1.0f));
        assertTrue(range.contains(10.0f));
        assertFalse(range.contains(0.0f));
        assertFalse(range.contains(11.0f));
    }

    @Test
    public void testAverage() throws Exception {
        assertEquals(2.0f, new Interval(1, 3).average(), Float.MIN_NORMAL);
        assertEquals(2.5f, new Interval(1, 4).average(), Float.MIN_NORMAL);
    }

    @Test
    public void testIterator() throws Exception {
        List<Integer> elements = new ArrayList<>();
        for (Integer index : new Interval(1, 10)) {
            elements.add(index);
        }
        assertEquals(10, elements.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), elements);
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, new Interval(1, 11).size());
        assertEquals(10, new Interval(1, 10).size());
    }

}
