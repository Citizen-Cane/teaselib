package teaselib.core;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
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

        // forenoon and noon don't in tersect because at 12 o'clock it's not forenoon anymore
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

        assertEquals(8, script.duration(Daytime.Evening, 0).remaining(TimeUnit.HOURS));
        assertEquals(8 + 24, script.duration(Daytime.Evening, 1).remaining(TimeUnit.HOURS));
        assertEquals(8 + 48, script.duration(Daytime.Evening, 2).remaining(TimeUnit.HOURS));

        assertEquals(19, script.duration(Daytime.Morning, 0).remaining(HOURS));
        assertEquals(19, script.duration(Daytime.Morning, 1).remaining(HOURS));
        assertEquals(19 + 24, script.duration(Daytime.Morning, 2).remaining(HOURS));
    }

    @Test
    public void testDaytimeEarierThan() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        testEarlierThan(script, Daytime.Morning, Daytime.Forenoon);
        testEarlierThan(script, Daytime.Noon, Daytime.Evening);
        testEarlierThan(script, Daytime.Morning, Daytime.Night);
    }

    private void testEarlierThan(TestScript script, Daytime now, Daytime later) {
        script.debugger.setTime(now);
        TimeOfDay timeOfDay = script.timeOfDay();
        assertTrue(timeOfDay.is(now));
        assertTrue(timeOfDay.isEarlierThan(later));
    }

    @Test
    public void testDaytimeLaterThan() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        testLaterThan(script, Daytime.Forenoon, Daytime.Morning);
        testLaterThan(script, Daytime.Evening, Daytime.Noon);
        testLaterThan(script, Daytime.Night, Daytime.Morning);
    }

    private void testLaterThan(TestScript script, Daytime later, Daytime dayTime) {
        script.debugger.setTime(later);
        TimeOfDay timeOfDay = script.timeOfDay();
        assertTrue(timeOfDay.is(later));
        assertTrue(timeOfDay.isLaterThan(dayTime));
    }

    @Test
    public void testDaytimeFromNow() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        // Around 8 o'clock
        script.debugger.setTime(Daytime.Evening);
        assertEquals(0, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));

        // around 8pm + 6h -> 2 o'clock in the morning
        assertEquals(6, script.duration(Daytime.Night).remaining(TimeUnit.HOURS));
    }

}
