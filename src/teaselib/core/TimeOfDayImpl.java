package teaselib.core;

import java.time.LocalDateTime;
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

    private static boolean is(LocalDateTime localDateTime, Daytime dayTime) {
        // TODO finds always the first interval,
        // but for overlapping intervals the first interval may not be the right one
        var interval = hours(dayTime);
        return is(localDateTime.toLocalTime(), interval);
    }

    private static boolean is(LocalDateTime localDateTime, Interval interval) {
        return is(localDateTime.toLocalTime(), interval);
    }

    static boolean is(LocalTime localTime, Interval interval) {
        float hour = localTime.getHour();
        return interval.contains(hour) || interval.contains(hour + 24.0f);
    }

    static Interval hours(Daytime dayTime) {
        return timeOfDayTable.get(dayTime);
    }

    // the number of days after now, e.g. today = 0; tomorrow = 1; yesterday = -1
    private final long days;
    private final LocalDateTime localDateTime;
    private final ItemLogger logger;

    TimeOfDayImpl(LocalDateTime localTime, long days, ItemLogger logger) {
        this.days = days;
        this.localDateTime = localTime;
        this.logger = logger;
    }

    @Override
    public boolean is(Daytime dayTime) {
        boolean value = is(localDateTime, dayTime);
        logger.log(this, "is", dayTime, value);
        return value;
    }

    @Override
    public boolean isEarlierThan(Daytime dayTime) {
        var thisDayTime = dayTimeEarly();
        boolean value = days * 24 + hours(thisDayTime).average() < hours(dayTime).average();
        logger.log(this, "isEarlierThan", dayTime, value);
        return value;
    }

    @Override
    public boolean isLaterThan(Daytime dayTime) {
        var thisDayTime = dayTimeLater();
        boolean value = days * 24 + hours(thisDayTime).average() > hours(dayTime).average();
        logger.log(this, "isLaterThan", dayTime, value);
        return value;
    }

    @Override
    public boolean isAnyOf(Daytime... dayTimes) {
        boolean value = Arrays.asList(dayTimes).stream().anyMatch(this::is);
        logger.log(this, "isAnyOf", dayTimes, value);
        return value;
    }

    private Daytime dayTimeEarly() {
        return timeOfDayTable.entrySet().stream()
                .filter(entry -> is(localDateTime, entry.getValue()))
                .map(Entry::getKey)
                .findFirst().orElseThrow();
    }

    private Daytime dayTimeLater() {
        List<Entry<Daytime, Interval>> entries = new ArrayList<>(timeOfDayTable.entrySet());
        Collections.reverse(entries);
        return entries.stream()
                .filter(entry -> is(localDateTime, entry.getValue()))
                .map(Entry::getKey)
                .findFirst().orElseThrow();
    }

    public static LocalDateTime getTime(TimeOfDay timeOfDay) {
        return ((TimeOfDayImpl) timeOfDay).localDateTime;
    }

    @Override
    public String toString() {
        return dayTimeEarly() + " " + localDateTime;
    }

}
