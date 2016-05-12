/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(1, timeLine.getTimeSpan(0.5).size());
        assertEquals(3, timeLine.getTimeSpan(4.0).size());
    }

    @Test
    public void testCapacityOverflowItems() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        timeLine.setCapacity(2, 60.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(0.5).size());
        assertEquals(2, timeLine.getTimeSpan(4.0).size());
        assertEquals(2, timeLine.getTimeSpan(60.0).size());
    }

    @Test
    public void testCapacityOverflowTimeSpan() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        timeLine.setCapacity(1000, 3 * 1000);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(0.5).size());
        assertEquals(2, timeLine.getTimeSpan(4.0).size());
        assertEquals(2, timeLine.getTimeSpan(60.0).size());
    }

    @Test
    public void testAppend() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertFalse(timeLine.add("start 2.0", 13.0));
        assertEquals(2, timeLine.getTimeSpan(3.0).size());
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(0.5).size());
        assertEquals(3, timeLine.getTimeSpan(4.0).size());
        assertEquals(3, timeLine.getTimeSpan(60.0).size());
    }

    @Test
    public void testAtLeastOneElement() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertTrue(timeLine.add("start 1.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(1.0).size());
    }

    @Test
    public void testLastN() {
        TimeLine<String> timeLine = new TimeLine<String>(10.0);
        assertEquals(0, timeLine.last(4).size());
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertEquals(1, timeLine.last(4).size());
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertFalse(timeLine.add("start 2.0", 13.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(3, timeLine.last(4).size());
        assertTrue(timeLine.add("start 5.0", 15.0));
        assertTrue(timeLine.add("start 6.0", 16.0));
        assertTrue(timeLine.add("start 7.0", 17.0));
        assertTrue(timeLine.add("start 8.0", 18.0));
        assertTrue(timeLine.add("start 9.0", 19.0));
        assertEquals(4, timeLine.last(4).size());
    }

}
