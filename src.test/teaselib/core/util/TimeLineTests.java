/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.devices.motiondetection.MotionDetectionResultImplementation;
import teaselib.motiondetection.MotionDetector;

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
        TimeLine<String> timeLine = new TimeLine<>(10.0);
        assertTrue(timeLine.add("start 1.0", 11.0));
        assertTrue(timeLine.add("start 2.0", 12.0));
        assertTrue(timeLine.add("start 4.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(0.5).size());
        assertEquals(3, timeLine.getTimeSpan(4.0).size());
    }

    @Test
    public void testCapacityOverflowItems() {
        TimeLine<String> timeLine = new TimeLine<>(10.0);
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
        TimeLine<String> timeLine = new TimeLine<>(10.0);
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
        TimeLine<String> timeLine = new TimeLine<>(10.0);
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
        TimeLine<String> timeLine = new TimeLine<>(10.0);
        assertTrue(timeLine.add("start 1.0", 14.0));
        assertEquals(1, timeLine.getTimeSpan(1.0).size());
    }

    @Test
    public void testLastN() {
        TimeLine<String> timeLine = new TimeLine<>(10.0);
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

    @Test
    public void testAmount() {
        MotionDetector.Presence motion = MotionDetector.Presence.Motion;
        MotionDetector.Presence noMotion = MotionDetector.Presence.NoMotion;
        TimeLine<Set<MotionDetector.Presence>> timeLine = new TimeLine<>(10.0);
        assertTrue(timeLine.add(new HashSet<>(Arrays.asList(motion)), 11.0));
        assertFalse(timeLine.add(new HashSet<>(Arrays.asList(motion)), 12.0));
        assertTrue(timeLine.add(new HashSet<>(Arrays.asList(noMotion)), 14.0));

        // Full coverage

        // "motion" is 2 seconds away from now, 0.5 coverage within 4.0 seconds
        assertEquals(0.5, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(4.0), motion), 0.0);
        // "noMotion" is 2.0 seconds away from now, 1.0 coverage within past 2.0
        // seconds
        assertEquals(1.0, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(2.0), noMotion),
                0.0);
        // "noMotion" is 2.0 seconds away from now, 0.5 coverage within past 4.0
        // seconds
        assertEquals(0.5, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(4.0), noMotion),
                0.0);

        // no coverage
        assertEquals(0.0, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(2.0), motion), 0.0);

        assertEquals(0.0, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(0.0), motion), 0.0);

        // partial coverage
        assertTrue(timeLine.add(new HashSet<>(Arrays.asList(motion)), 15.0));

        assertEquals(0.6, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(5.0), motion), 0.0);
        assertEquals(0.4, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(5.0), noMotion),
                0.0);

        assertEquals(0.5, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(2.0), motion), 0.0);
        assertEquals(0.5, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(2.0), noMotion),
                0.0);

        assertEquals(0.33, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(3.0), motion),
                0.01);
        assertEquals(0.66, MotionDetectionResultImplementation.getAmount(timeLine.getTimeSpanSlices(3.0), noMotion),
                0.01);

    }
}
