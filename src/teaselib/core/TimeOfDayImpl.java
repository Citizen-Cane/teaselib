package teaselib.core;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

import teaselib.util.Daytime;
import teaselib.util.Interval;

/**
 * @author Citizen-Cane
 *
 */
public class TimeOfDayImpl implements TimeOfDay {

    static Map<Daytime, Interval> defaultTimeOfDayTable() {
        EnumMap<Daytime, Interval> timeOfDay = new EnumMap<>(Daytime.class);

        timeOfDay.put(Daytime.Morning, new Interval(6, 9));
        timeOfDay.put(Daytime.Forenoon, new Interval(8, 11));
        timeOfDay.put(Daytime.Noon, new Interval(12, 13));
        timeOfDay.put(Daytime.Afternoon, new Interval(13, 18));
        timeOfDay.put(Daytime.Evening, new Interval(18, 23));
        timeOfDay.put(Daytime.Night, new Interval(-2, 5));

        return timeOfDay;
    }

    static final Map<Daytime, Interval> timeOfDayTable = defaultTimeOfDayTable();

    static boolean is(LocalTime localTime, Daytime timeOfDay) {
        return is(localTime, timeOfDay, timeOfDayTable);
    }

    static boolean is(LocalTime localTime, Daytime timeOfDay, Map<Daytime, Interval> defaultTimeOfDayTable) {
        Interval interval = defaultTimeOfDayTable.get(timeOfDay);
        return interval.contains(localTime.getHour()) || interval.contains(localTime.getHour() - 24);
    }

    private final LocalTime localTime;

    TimeOfDayImpl(LocalTime localTime) {
        this.localTime = localTime;
    }

    @Override
    public boolean is(Daytime dayTime) {
        return is(localTime, dayTime);
    }
}
