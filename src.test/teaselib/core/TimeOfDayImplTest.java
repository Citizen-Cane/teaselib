package teaselib.core;

import static org.junit.Assert.*;

import java.time.LocalTime;

import org.junit.Test;

import teaselib.util.TimeOfDay;

public class TimeOfDayImplTest {
    @Test
    public void testDefaultTable() {
        assertTrue(TimeOfDayImpl.is(LocalTime.of(4, 0), TimeOfDay.Night));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(6, 0), TimeOfDay.Night));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(6, 0), TimeOfDay.Morning));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), TimeOfDay.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(8, 0), TimeOfDay.Forenoon));

        assertFalse(TimeOfDayImpl.is(LocalTime.of(10, 0), TimeOfDay.Morning));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(10, 0), TimeOfDay.Forenoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(11, 0), TimeOfDay.Forenoon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(12, 0), TimeOfDay.Forenoon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(12, 0), TimeOfDay.Noon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(13, 0), TimeOfDay.Noon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(14, 0), TimeOfDay.Noon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(13, 0), TimeOfDay.Afternoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(14, 0), TimeOfDay.Afternoon));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(18, 0), TimeOfDay.Afternoon));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(19, 0), TimeOfDay.Afternoon));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(18, 0), TimeOfDay.Evening));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(19, 0), TimeOfDay.Evening));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(23, 0), TimeOfDay.Evening));
        assertFalse(TimeOfDayImpl.is(LocalTime.of(0, 0), TimeOfDay.Evening));

        assertTrue(TimeOfDayImpl.is(LocalTime.of(22, 0), TimeOfDay.Night));
        assertTrue(TimeOfDayImpl.is(LocalTime.of(0, 0), TimeOfDay.Night));
    }
}
