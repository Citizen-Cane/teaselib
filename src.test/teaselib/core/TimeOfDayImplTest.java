package teaselib.core;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.TimeOfDayImpl.hours;

import java.time.LocalTime;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;
import teaselib.util.Daytime;

public class TimeOfDayImplTest {
    @Test
    public void testDefaultTable() {
        assertTrue(TimeOfDayImpl.is(LocalTime.of(4, 0), Daytime.Night));
        // 5 o'clock is also still night, but only from the previous evening's view
        assertTrue(TimeOfDayImpl.is(LocalTime.of(5, 0), Daytime.Night));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(6, 0), Daytime.Night));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(5, 0), Daytime.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(6, 0), Daytime.Morning));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), Daytime.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), Daytime.Forenoon));

        assertFalse(TimeOfDayImpl.is(LocalTime.of(10, 0), Daytime.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(10, 0), Daytime.Forenoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(11, 0), Daytime.Forenoon));

        // forenoon and noon don't intersect because at 12 o'clock it's not forenoon anymore
        assertFalse(TimeOfDayImpl.is(LocalTime.of(12, 0), Daytime.Forenoon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(12, 0), Daytime.Noon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(13, 0), Daytime.Noon));
        // Noon ends before 2pn
        assertFalse(TimeOfDayImpl.is(LocalTime.of(14, 0), Daytime.Noon));

        assertFalse(TimeOfDayImpl.is(LocalTime.of(13, 0), Daytime.Afternoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(14, 0), Daytime.Afternoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(18, 0), Daytime.Afternoon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(19, 0), Daytime.Afternoon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(18, 0), Daytime.Evening));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(19, 0), Daytime.Evening));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(23, 0), Daytime.Evening));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(0, 0), Daytime.Evening));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(22, 0), Daytime.Night));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(0, 0), Daytime.Night));
    }

    @Test
    public void testAnyOf() {
        assertTrue(new TimeOfDayImpl(LocalTime.of(4, 0)).isAnyOf(Daytime.Night, Daytime.Forenoon));
        assertFalse(new TimeOfDayImpl(LocalTime.of(4, 0)).isAnyOf(Daytime.Morning, Daytime.Forenoon));
    }

    @Test
    public void testHours() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.debugger.setTime(Daytime.Morning);
        assertTrue(script.timeOfDay().is(Daytime.Morning));
        int morning = 7;
        // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
        assertEquals(morning, new Date(script.debugger.teaseLib.getTime(TimeUnit.MILLISECONDS)).getHours());
        int h = 0;
        int m = 0;
        try {
            for (h = 0; h < 24; h++) {
                for (m = 0; m < 60; m += 15) {
                    TimeOfDay timeOfDay = script.timeOfDay();
                    assertNotNull(timeOfDay);
                    assertTrue(timeOfDay.isAnyOf(Daytime.values()));
                    script.debugger.advanceTime(15, TimeUnit.MINUTES);
                }
            }
        } catch (NoSuchElementException e) {
            fail("Failed @ " + (morning + h) + ":" + m);
        }
    }

    @Test
    public void testFutureDaytime() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        for (Daytime daytime : Daytime.values()) {
            script.debugger.setTime(daytime);
            assertTrue(script.timeOfDay().is(daytime));
        }
    }

    @Test
    public void testDaytimeDurationForSomeDays() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.debugger.setTime(Daytime.Noon);
        // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
        assertEquals(4, script.duration(Daytime.Afternoon).remaining(TimeUnit.HOURS));
        assertEquals(8, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));
        assertEquals(14, script.duration(Daytime.Night).remaining(TimeUnit.HOURS));
        assertEquals(19, script.duration(Daytime.Morning).remaining(TimeUnit.HOURS));
        assertEquals(21, script.duration(Daytime.Forenoon).remaining(TimeUnit.HOURS));

        testFromNoon(script, 0);
        testFromNoon(script, 1);
        testFromNoon(script, 2);
    }

    private static void testFromNoon(TestScript script, long days) {
        long today = days * 24;
        assertEquals(4 + today, script.duration(Daytime.Afternoon, days).remaining(TimeUnit.HOURS));
        assertEquals(8 + today, script.duration(Daytime.Evening, days).remaining(TimeUnit.HOURS));
        long tomorrow = Math.max(today - 24, 0);
        assertEquals(14 + tomorrow, script.duration(Daytime.Night, days).remaining(TimeUnit.HOURS));
        assertEquals(19 + tomorrow, script.duration(Daytime.Morning, days).remaining(TimeUnit.HOURS));
        assertEquals(21 + tomorrow, script.duration(Daytime.Forenoon, days).remaining(TimeUnit.HOURS));
    }

    @Test
    public void testDaytimeDurationUntilTomorrow() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.debugger.setTime(Daytime.Noon);

        // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
        assertEquals(8, script.duration(Daytime.Evening, 0).remaining(TimeUnit.HOURS));
        assertEquals(8 + 24, script.duration(Daytime.Evening, 1).remaining(TimeUnit.HOURS));
        assertEquals(8 + 48, script.duration(Daytime.Evening, 2).remaining(TimeUnit.HOURS));

        assertEquals(19, script.duration(Daytime.Morning, 0).remaining(HOURS));
        assertEquals(19, script.duration(Daytime.Morning, 1).remaining(HOURS));
        assertEquals(19 + 24, script.duration(Daytime.Morning, 2).remaining(HOURS));
    }

    @Test
    public void testDaytimeOverlapping() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        assertTrue(hours(Daytime.Morning).intersects(hours(Daytime.Forenoon)));
        TimeOfDay morning = timeOfDay(script, Daytime.Morning);
        assertTrue(morning.is(Daytime.Morning));
        assertTrue(morning.isEarlierThan(Daytime.Forenoon));
    }

    @Test
    public void testDaytimeEarlierThan() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        assertTrue(timeOfDay(script, Daytime.Morning).isEarlierThan(Daytime.Forenoon));
        assertTrue(timeOfDay(script, Daytime.Forenoon).isEarlierThan(Daytime.Noon));
        assertTrue(timeOfDay(script, Daytime.Noon).isEarlierThan(Daytime.Afternoon));
        assertTrue(timeOfDay(script, Daytime.Afternoon).isEarlierThan(Daytime.Evening));
        assertTrue(timeOfDay(script, Daytime.Evening).isEarlierThan(Daytime.Night));

        // TODO which one is right?
        assertTrue(timeOfDay(script, Daytime.Morning).isEarlierThan(Daytime.Night));
        // assertTrue(timeOfDay(script, Daytime.Night).isEarlierThan(Daytime.Morning));

        script.debugger.setTime(Daytime.Morning);
        script.debugger.advanceTime(1, DAYS);
        TimeOfDay morning = script.timeOfDay();
        assertTrue(morning.is(Daytime.Morning));
        assertTrue(morning.isEarlierThan(Daytime.Night));
    }

    @Test
    public void testDaytimeLaterThan() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        assertTrue(timeOfDay(script, Daytime.Forenoon).isLaterThan(Daytime.Morning));
        assertTrue(timeOfDay(script, Daytime.Noon).isLaterThan(Daytime.Forenoon));
        assertTrue(timeOfDay(script, Daytime.Afternoon).isLaterThan(Daytime.Noon));
        assertTrue(timeOfDay(script, Daytime.Evening).isLaterThan(Daytime.Afternoon));
        assertTrue(timeOfDay(script, Daytime.Night).isLaterThan(Daytime.Evening));

        // TODO which one is right?
        // assertTrue(timeOfDay(script, Daytime.Morning).isLaterThan(Daytime.Night));
        assertTrue(timeOfDay(script, Daytime.Night).isLaterThan(Daytime.Morning));

        script.debugger.setTime(Daytime.Night);
        script.debugger.advanceTime(1, DAYS);
        TimeOfDay night = script.timeOfDay();
        assertTrue(night.is(Daytime.Night));
        assertTrue(night.isLaterThan(Daytime.Morning));
    }

    private static TimeOfDay timeOfDay(TestScript script, Daytime dayTime) {
        script.debugger.setTime(dayTime);
        return script.timeOfDay();
    }

    @Test
    public void testDaytimeFromNow() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        // Around 8 o'clock
        script.debugger.setTime(Daytime.Evening);
        // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
        assertEquals(0, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));

        // around 8pm + 6h -> 2 o'clock in the morning
        assertEquals(6, script.duration(Daytime.Night).remaining(TimeUnit.HOURS));
    }

}
