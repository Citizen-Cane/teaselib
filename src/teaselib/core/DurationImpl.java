package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;

public class DurationImpl implements Duration {
    private final TeaseLib teaseLib;
    private final long start;
    private final long limits;

    public DurationImpl(TeaseLib teaseLib) {
        this(teaseLib, 0, TeaseLib.DURATION_TIME_UNIT);
    }

    public DurationImpl(TeaseLib teaseLib, long limit, TimeUnit unit) {
        this.teaseLib = teaseLib;
        this.start = this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
        this.limits = convertToMillis(limit, unit);
    }

    public DurationImpl(TeaseLib teaseLib, long start, long limit, TimeUnit unit) {
        this.teaseLib = teaseLib;
        this.start = convertToMillis(start, unit);
        this.limits = convertToMillis(limit, unit);
    }

    private static long convertToMillis(long value, TimeUnit unit) {
        if (value == Long.MIN_VALUE)
            return Long.MIN_VALUE;
        if (value == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        else
            return TeaseLib.DURATION_TIME_UNIT.convert(value, unit);
    }

    public long convertToUnit(long value, TimeUnit unit) {
        if (value == Long.MIN_VALUE)
            return Long.MIN_VALUE;
        if (value == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        else
            return unit.convert(value, TeaseLib.DURATION_TIME_UNIT);
    }

    @Override
    public long start(TimeUnit unit) {
        return convertToUnit(start, unit);
    }

    @Override
    public long limit(TimeUnit unit) {
        return convertToUnit(limits, unit);
    }

    @Override
    public long elapsed(TimeUnit unit) {
        long now = this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
        return convertToUnit(now - start, unit);
    }

    @Override
    public long remaining(TimeUnit unit) {
        return convertToUnit(limits - elapsed(TeaseLib.DURATION_TIME_UNIT), unit);
    }

    @Override
    public long end(TimeUnit unit) {
        if (limits >= Long.MAX_VALUE - start) {
            return Long.MAX_VALUE;
        } else {
            return convertToUnit(start + limits, unit);
        }
    }

    @Override
    public boolean expired() {
        return this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT) - start >= limits;
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS)) + "+" + limits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (limits ^ (limits >>> 32));
        result = prime * result + (int) (start ^ (start >>> 32));
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
        DurationImpl other = (DurationImpl) obj;
        if (limits != other.limits)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
}