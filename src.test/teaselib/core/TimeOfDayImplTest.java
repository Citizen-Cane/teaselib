package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;
import teaselib.util.Daytime;

public class TimeOfDayImplTest {
    @Test
    public void testDefaultTable() {
        assertTrue(TimeOfDayImpl.is(LocalTime.of(4, 0), Daytime.Night));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(6, 0), Daytime.Night));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(6, 0), Daytime.Morning));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), Daytime.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), Daytime.Forenoon));

        assertFalse(TimeOfDayImpl.is(LocalTime.of(10, 0), Daytime.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(10, 0), Daytime.Forenoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(11, 0), Daytime.Forenoon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(12, 0), Daytime.Forenoon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(12, 0), Daytime.Noon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(13, 0), Daytime.Noon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(14, 0), Daytime.Noon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(13, 0), Daytime.Afternoon));
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
        assertEquals(9, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));
        assertEquals(14, script.duration(Daytime.Night).remaining(TimeUnit.HOURS));
        assertEquals(20, script.duration(Daytime.Morning).remaining(TimeUnit.HOURS));
        assertEquals(22, script.duration(Daytime.Forenoon).remaining(TimeUnit.HOURS));

        testFromNoon(script, 0);
        testFromNoon(script, 1);
        testFromNoon(script, 2);
    }

    private static void testFromNoon(TestScript script, long days) {
        long today = days * 24;
        assertEquals(4 + today, script.duration(Daytime.Afternoon, days).remaining(TimeUnit.HOURS));
        assertEquals(9 + today, script.duration(Daytime.Evening, days).remaining(TimeUnit.HOURS));
        long tomorrow = Math.max(today - 24, 0);
        assertEquals(14 + tomorrow, script.duration(Daytime.Night, days).remaining(TimeUnit.HOURS));
        assertEquals(20 + tomorrow, script.duration(Daytime.Morning, days).remaining(TimeUnit.HOURS));
        assertEquals(22 + tomorrow, script.duration(Daytime.Forenoon, days).remaining(TimeUnit.HOURS));
    }

    @Test
    public void testDaytimeDurationUntilTomorrow() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.debugger.setTime(Daytime.Noon);

        assertEquals(9, script.duration(Daytime.Evening, 0).remaining(TimeUnit.HOURS));
        assertEquals(9 + 24, script.duration(Daytime.Evening, 1).remaining(TimeUnit.HOURS));
        assertEquals(9 + 48, script.duration(Daytime.Evening, 2).remaining(TimeUnit.HOURS));

        assertEquals(20, script.duration(Daytime.Morning, 0).remaining(HOURS));
        assertEquals(20, script.duration(Daytime.Morning, 1).remaining(HOURS));
        assertEquals(20 + 24, script.duration(Daytime.Morning, 2).remaining(HOURS));
    }

}
