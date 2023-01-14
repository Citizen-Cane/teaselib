package teaselib.core;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;

public abstract class AbstractDuration implements Duration {

    final TeaseLib teaseLib;

    final long start;
    final long limit;

    AbstractDuration(TeaseLib teaseLib, long start, long limit) {
        this.teaseLib = teaseLib;

        if (start < 0) {
            throw new IllegalArgumentException("Duration limit must be 0 or positive: " + Long.toString(limit));
        }
        this.start = start;

        if (limit < 0) {
            throw new IllegalArgumentException("Duration limit must be 0 or positive: " + Long.toString(limit));
        }
        this.limit = limit;
    }

    static long convertToDuration(long value, TimeUnit unit) {
        if (value == Long.MIN_VALUE)
            return Long.MIN_VALUE;
        if (value == Duration.INFINITE)
            return Duration.INFINITE;
        else
            return TeaseLib.DURATION_TIME_UNIT.convert(value, unit);
    }

    static long convertToUnit(long value, TimeUnit unit) {
        if (value == Long.MIN_VALUE)
            return Long.MIN_VALUE;
        if (value == Duration.INFINITE)
            return Duration.INFINITE;
        else
            return unit.convert(value, TeaseLib.DURATION_TIME_UNIT);
    }

    @Override
    public long start(TimeUnit unit) {
        return convertToUnit(start, unit);
    }

    @Override
    public TimeOfDay start() {
        return timeOfDay(start);
    }

    @Override
    public long remaining(TimeUnit unit) {
        return limit(unit) - elapsed(unit);
    }

    @Override
    public long limit(TimeUnit unit) {
        return convertToUnit(limit, unit);
    }

    @Override
    public TimeOfDay limit() {
        return timeOfDay(start + limit);
    }

    @Override
    public long end(TimeUnit unit) {
        long s = start(unit);
        long e = elapsed(unit);
        if (s >= Duration.INFINITE - e) {
            return Duration.INFINITE;
        } else {
            return s + e;
        }
    }

    @Override
    public TimeOfDay end() {
        return timeOfDay(end(TeaseLib.DURATION_TIME_UNIT));
    }

    private TimeOfDay timeOfDay(long time) {
        var fromDate = TeaseLib.localDateTime(teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT), TeaseLib.DURATION_TIME_UNIT);
        var toDate = TeaseLib.localDateTime(time, TeaseLib.DURATION_TIME_UNIT);
        long days = ChronoUnit.DAYS.between(
                LocalDate.ofYearDay(fromDate.getYear(), fromDate.getDayOfYear()),
                LocalDate.ofYearDay(toDate.getYear(), toDate.getDayOfYear()));
        return teaseLib.timeOfDay(toDate, days);
    }

    @Override
    public boolean expired() {
        return elapsed(TeaseLib.DURATION_TIME_UNIT) >= limit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (limit ^ (limit >>> 32));
        result = prime * result + (int) (start ^ (start >>> 32));
        result = prime * result + ((teaseLib == null) ? 0 : teaseLib.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractDuration other = (AbstractDuration) obj;
        if (limit != other.limit)
            return false;
        if (start != other.start)
            return false;
        return teaseLib == other.teaseLib;
    }

}
