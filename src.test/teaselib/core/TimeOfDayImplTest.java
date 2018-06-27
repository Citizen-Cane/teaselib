package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;

import org.junit.Test;

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
}
