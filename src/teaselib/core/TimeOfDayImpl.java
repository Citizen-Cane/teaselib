package teaselib.core;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import teaselib.util.Daytime;
import teaselib.util.Interval;

/**
 * @author Citizen-Cane
 *
 */
public class TimeOfDayImpl implements TimeOfDay {

    static Map<Daytime, Interval> defaultTimeOfDayTable() {
        EnumMap<Daytime, Interval> timeOfDay = new EnumMap<>(Daytime.class);

        // hour intervals last til the last minute of that hour
        timeOfDay.put(Daytime.Morning, new Interval(5, 9));
        timeOfDay.put(Daytime.Forenoon, new Interval(8, 11));
        timeOfDay.put(Daytime.Noon, new Interval(12, 13));
        timeOfDay.put(Daytime.Afternoon, new Interval(14, 18));
        timeOfDay.put(Daytime.Evening, new Interval(18, 23));
        timeOfDay.put(Daytime.Night, new Interval(22, 30));
        // TODO interval values are integers -> interval.average() is not exact for odd span (half hours)

        return timeOfDay;
    }

    static final Map<Daytime, Interval> timeOfDayTable = defaultTimeOfDayTable();

    static boolean is(LocalTime localTime, Daytime dayTime) {
        return is(localTime, dayTime, timeOfDayTable);
    }

    static boolean is(LocalTime localTime, Daytime dayTimes, Map<Daytime, Interval> defaultTimeOfDayTable) {
        Interval interval = defaultTimeOfDayTable.get(dayTimes);
        float hour = localTime.getHour() + localTime.getMinute() / 60.0f;
        return interval.contains(hour) || interval.contains(hour + 24.0f);
    }

    static Interval hours(Daytime dayTime) {
        return timeOfDayTable.entrySet().stream().filter(entry -> entry.getKey() == dayTime).map(Entry::getValue)
                .findFirst().orElseThrow();
    }

    final LocalTime localTime;

    TimeOfDayImpl(LocalTime localTime) {
        this.localTime = localTime;
    }

    @Override
    public boolean is(Daytime dayTime) {
        return is(localTime, dayTime);
    }

    @Override
    public boolean isEarlierThan(Daytime dayTime) {
        return timeOfDayTable.get(dayTime()).average() < timeOfDayTable.get(dayTime).average();
    }

    @Override
    public boolean isLaterThan(Daytime dayTime) {
        return timeOfDayTable.get(dayTime()).average() > timeOfDayTable.get(dayTime).average();
    }

    @Override
    public boolean isAnyOf(Daytime... daytimes) {
        return Arrays.asList(daytimes).stream().anyMatch(this::is);
    }

    @Override
    public Daytime dayTime() {
        return timeOfDayTable.entrySet().stream().map(Entry::getKey).filter(this::is).findFirst().orElseThrow();
    }

    public static LocalTime getTime(TimeOfDay timeOfDay) {
        return ((TimeOfDayImpl) timeOfDay).localTime;
    }

    @Override
    public String toString() {
        return dayTime() + " " + localTime;
    }

}
