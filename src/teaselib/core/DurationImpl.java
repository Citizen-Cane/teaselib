package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.util.DurationFormat;

public class DurationImpl implements Duration {
    private final TeaseLib teaseLib;
    private final long start;
    private final long limit;

    public DurationImpl(TeaseLib teaseLib) {
        this(teaseLib, 0, TeaseLib.DURATION_TIME_UNIT);
    }

    public DurationImpl(TeaseLib teaseLib, long limit, TimeUnit unit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Duration limit must be 0 or positive: " + Long.toString(limit));
        }

        this.teaseLib = teaseLib;
        this.start = this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
        this.limit = convertToMillis(limit, unit);
    }

    public DurationImpl(TeaseLib teaseLib, long start, long limit, TimeUnit unit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Duration limit must be 0 or positive: " + Long.toString(limit));
        }

        this.teaseLib = teaseLib;
        this.start = convertToMillis(start, unit);
        this.limit = convertToMillis(limit, unit);
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
        return convertToUnit(limit, unit);
    }

    @Override
    public long elapsed(TimeUnit unit) {
        long now = this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT);
        return convertToUnit(now - start, unit);
    }

    @Override
    public long remaining(TimeUnit unit) {
        return convertToUnit(limit - elapsed(TeaseLib.DURATION_TIME_UNIT), unit);
    }

    @Override
    public long end(TimeUnit unit) {
        if (limit >= Long.MAX_VALUE - start) {
            return Long.MAX_VALUE;
        } else {
            return convertToUnit(start + limit, unit);
        }
    }

    @Override
    public boolean expired() {
        return this.teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT) - start >= limit;
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS))
                + (limit > 0 ? "+" + DurationFormat.toString(limit, TeaseLib.DURATION_TIME_UNIT) : "");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (limit ^ (limit >>> 32));
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
        if (limit != other.limit)
            return false;
        if (start != other.start)
            return false;
        return true;
    }
}