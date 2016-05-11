/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author someone
 *
 */
public class TimeLineTests {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testAdd() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.tail(0.5).size());
        assertEquals(3, timeLine.tail(4.0).size());
    }

    @Test
    public void testCapacityOverflowItems() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        timeLine.setCapacity(2, 60.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.tail(0.5).size());
        assertEquals(2, timeLine.tail(4.0).size());
        assertEquals(2, timeLine.tail(60.0).size());
    }

    @Test
    public void testCapacityOverflowTimeSpan() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        timeLine.setCapacity(1000, 3 * 1000);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.tail(0.5).size());
        assertEquals(2, timeLine.tail(4.0).size());
        assertEquals(2, timeLine.tail(60.0).size());
    }

    @Test
    public void testAppend() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertFalse(timeLine.add("start 2.0", 13.0));
        assertEquals(2, timeLine.tail(3.0).size());
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.tail(0.5).size());
        assertEquals(3, timeLine.tail(4.0).size());
        assertEquals(3, timeLine.tail(60.0).size());
    }

    @Test
    public void testAtLeastOneElement() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertTrue(timeLine.add("start 1.0", 14.0));
        assertEquals(1, timeLine.tail(1.0).size());
    }

}
