package teaselib.core;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

import teaselib.util.Interval;
import teaselib.util.TimeOfDay;

/**
 * @author Citizen-Cane
 *
 */
public class TimeOfDayImpl {
    static final Map<TimeOfDay, Interval> defaultTimeOfDayTable = createDefaultTimeOfDayTable();

    static Map<TimeOfDay, Interval> createDefaultTimeOfDayTable() {
        EnumMap<TimeOfDay, Interval> timeOfDay = new EnumMap<>(TimeOfDay.class);

        timeOfDay.put(TimeOfDay.Morning, new Interval(6, 9));
        timeOfDay.put(TimeOfDay.Forenoon, new Interval(8, 11));
        timeOfDay.put(TimeOfDay.Noon, new Interval(12, 13));
        timeOfDay.put(TimeOfDay.Afternoon, new Interval(13, 18));
        timeOfDay.put(TimeOfDay.Evening, new Interval(18, 23));
        timeOfDay.put(TimeOfDay.Night, new Interval(-2, 5));

        return timeOfDay;
    }

    private TimeOfDayImpl() {
    }

    static boolean is(LocalTime localTime, TimeOfDay timeOfDay) {
        return is(localTime, timeOfDay, defaultTimeOfDayTable);
    }

    static boolean is(LocalTime localTime, TimeOfDay timeOfDay, Map<TimeOfDay, Interval> defaultTimeOfDayTable) {
        Interval interval = defaultTimeOfDayTable.get(timeOfDay);
        return interval.contains(localTime.getHour()) || interval.contains(localTime.getHour() - 24);
    }
}
