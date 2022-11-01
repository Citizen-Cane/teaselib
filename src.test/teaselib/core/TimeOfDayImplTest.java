package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static teaselib.core.TimeOfDayImpl.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        assertTrue(is(LocalTime.of(4, 0), Daytime.Night));
        // 5 o'clock is also still night, but only from the previous evening's view
        assertTrue(is(LocalTime.of(5, 0), Daytime.Night));
        assertTrue(is(LocalTime.of(6, 0), Daytime.Night));

        assertTrue(is(LocalTime.of(5, 0), Daytime.Morning));
        assertTrue(is(LocalTime.of(6, 0), Daytime.Morning));

        assertTrue(is(LocalTime.of(8, 0), Daytime.Morning));
        assertTrue(is(LocalTime.of(8, 0), Daytime.Forenoon));

        assertFalse(is(LocalTime.of(10, 0), Daytime.Morning));
        assertTrue(is(LocalTime.of(10, 0), Daytime.Forenoon));
        assertTrue(is(LocalTime.of(11, 0), Daytime.Forenoon));

        // forenoon and noon don't intersect because at 12 o'clock it's not forenoon anymore
        assertFalse(is(LocalTime.of(12, 0), Daytime.Forenoon));

        assertTrue(is(LocalTime.of(12, 0), Daytime.Noon));
        assertTrue(is(LocalTime.of(13, 0), Daytime.Noon));
        // Noon ends before 2pn
        assertFalse(is(LocalTime.of(14, 0), Daytime.Noon));

        assertFalse(is(LocalTime.of(13, 0), Daytime.Afternoon));
        assertTrue(is(LocalTime.of(14, 0), Daytime.Afternoon));
        assertTrue(is(LocalTime.of(18, 0), Daytime.Afternoon));
        assertFalse(is(LocalTime.of(19, 0), Daytime.Afternoon));

        assertTrue(is(LocalTime.of(18, 0), Daytime.Evening));
        assertTrue(is(LocalTime.of(19, 0), Daytime.Evening));
        assertTrue(is(LocalTime.of(23, 0), Daytime.Evening));
        assertFalse(is(LocalTime.of(0, 0), Daytime.Evening));

        assertTrue(is(LocalTime.of(22, 0), Daytime.Night));
        assertTrue(is(LocalTime.of(0, 0), Daytime.Night));
    }

    static boolean is(LocalTime localTime, Daytime dayTime) {
        // TODO finds always the first interval,
        // but for overlapping intervals the first interval may not be the right one
        var interval = hours(dayTime);
        return TimeOfDayImpl.is(localTime, interval);
    }

    @Test
    public void testAnyOf() {
        LocalDate today = LocalDate.now();
        assertTrue(new TimeOfDayImpl(LocalDateTime.of(today, LocalTime.of(4, 0)), 0).isAnyOf(Daytime.Night, Daytime.Forenoon));
        assertFalse(new TimeOfDayImpl(LocalDateTime.of(today, LocalTime.of(4, 0)), 0).isAnyOf(Daytime.Morning, Daytime.Forenoon));
    }

    @Test
    public void testHours() throws IOException {
        try (TestScript script = new TestScript()) {
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
    }

    @Test
    public void testFutureDaytime() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            for (Daytime daytime : Daytime.values()) {
                script.debugger.setTime(daytime);
                assertTrue(script.timeOfDay().is(daytime));
            }
        }
    }

    @Test
    public void testDaytimeDurationForSomeDays() throws IOException {
        try (TestScript script = new TestScript()) {
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
    public void testDaytimeDurationUntilTomorrow() throws IOException {
        try (TestScript script = new TestScript()) {
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
    }

    @Test
    public void testDaytimeOverlapping() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            assertTrue(hours(Daytime.Morning).intersects(hours(Daytime.Forenoon)));
            TimeOfDay morning = timeOfDay(script, Daytime.Morning);
            assertTrue(morning.is(Daytime.Morning));
            assertTrue(morning.isEarlierThan(Daytime.Forenoon));
        }
    }

    @Test
    public void testDaytimeEarlierThan() throws IOException {
        try (TestScript script = new TestScript()) {
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
    }

    @Test
    public void testDaytimeLaterThanTomorrow() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            {
                var duration = script.duration(Daytime.Morning, 1);
                var nextMorning = duration.limit();
                assertTrue(nextMorning.isLaterThan(Daytime.Night));
            }

            {
                var duration = script.duration(Daytime.Noon, 1);
                var nextNoon = duration.limit();
                assertTrue(nextNoon.isLaterThan(Daytime.Night));
            }
        }
    }

    private static TimeOfDay timeOfDay(TestScript script, Daytime dayTime) {
        script.debugger.setTime(dayTime);
        return script.timeOfDay();
    }

    @Test
    public void testDaytimeFromNow() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            // Around 8 o'clock -> same day
            script.debugger.setTime(Daytime.Noon);
            // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
            assertEquals(8, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));
            assertEquals(32, script.duration(Daytime.Evening, 1).remaining(TimeUnit.HOURS));
            assertEquals(56, script.duration(Daytime.Evening, 2).remaining(TimeUnit.HOURS));

            // Around 8 o'clock -> same day
            script.debugger.setTime(Daytime.Evening);
            // Fails on transition dates (e.g. European Summer Time) since such days will have different lengths
            assertEquals(0, script.duration(Daytime.Evening).remaining(TimeUnit.HOURS));
            assertEquals(24, script.duration(Daytime.Evening, 1).remaining(TimeUnit.HOURS));
            assertEquals(48, script.duration(Daytime.Evening, 2).remaining(TimeUnit.HOURS));

            // around 8pm + 6h -> 2 o'clock in the morning -> next day
            assertEquals(6, script.duration(Daytime.Night).remaining(TimeUnit.HOURS));
            assertEquals(6, script.duration(Daytime.Night, 1).remaining(TimeUnit.HOURS));
            assertEquals(30, script.duration(Daytime.Night, 2).remaining(TimeUnit.HOURS));

            // around 8pm + 11h -> 7 o'clock in the morning -> next day
            assertEquals(11, script.duration(Daytime.Morning).remaining(TimeUnit.HOURS));
            assertEquals(11, script.duration(Daytime.Morning, 1).remaining(TimeUnit.HOURS));
            assertEquals(35, script.duration(Daytime.Morning, 2).remaining(TimeUnit.HOURS));
        }
    }

    @Test
    public void testDurationDaytime() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            script.debugger.setTime(Daytime.Morning);

            var duration = script.duration(Daytime.Noon);
            assertTrue(duration.start().is(Daytime.Morning));
            assertTrue(duration.end().is(Daytime.Morning));
            assertTrue(duration.limit().is(Daytime.Noon));

            script.debugger.advanceTime(8, HOURS);
            assertTrue(duration.start().is(Daytime.Morning));
            assertTrue(duration.end().is(Daytime.Afternoon));
            assertTrue(duration.limit().is(Daytime.Noon));
        }
    }

}
