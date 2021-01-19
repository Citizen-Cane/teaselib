package teaselib.core;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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

        return timeOfDay;
    }

    static final Map<Daytime, Interval> timeOfDayTable = defaultTimeOfDayTable();

    static boolean is(LocalTime localTime, Daytime dayTime) {
        // TODO finds always the first interval but for overlapping intervals the first interval may not be the right
        // one
        Interval interval = hours(dayTime);
        return is(localTime, interval);
    }

    private static boolean is(LocalTime localTime, Interval interval) {
        float hour = localTime.getHour();
        return interval.contains(hour) || interval.contains(hour + 24.0f);
    }

    static Interval hours(Daytime dayTime) {
        return timeOfDayTable.get(dayTime);
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
        Daytime thisDayTime = dayTimeEarly();
        return hours(thisDayTime).average() < hours(dayTime).average();
    }

    @Override
    public boolean isLaterThan(Daytime dayTime) {
        Daytime thisDayTime = dayTimeLater();
        return hours(thisDayTime).average() > hours(dayTime).average();
    }

    @Override
    public boolean isAnyOf(Daytime... daytimes) {
        return Arrays.asList(daytimes).stream().anyMatch(this::is);
    }

    private Daytime dayTimeEarly() {
        return timeOfDayTable.entrySet().stream().filter(entry -> is(localTime, entry.getValue())).map(Entry::getKey)
                .findFirst().orElseThrow();
    }

    private Daytime dayTimeLater() {
        List<Entry<Daytime, Interval>> entries = new ArrayList<>(timeOfDayTable.entrySet());
        Collections.reverse(entries);
        return entries.stream().filter(entry -> is(localTime, entry.getValue())).map(Entry::getKey).findFirst()
                .orElseThrow();
    }

    public static LocalTime getTime(TimeOfDay timeOfDay) {
        return ((TimeOfDayImpl) timeOfDay).localTime;
    }

    @Override
    public String toString() {
        return dayTimeEarly() + " " + localTime;
    }

}
