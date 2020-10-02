package teaselib.core;

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
        if (value == Long.MAX_VALUE)
            return Long.MAX_VALUE;
        else
            return TeaseLib.DURATION_TIME_UNIT.convert(value, unit);
    }

    static long convertToUnit(long value, TimeUnit unit) {
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
    public long remaining(TimeUnit unit) {
        return limit(unit) - elapsed(unit);
    }

    @Override
    public long limit(TimeUnit unit) {
        return convertToUnit(limit, unit);
    }

    @Override
    public long end(TimeUnit unit) {
        long s = start(unit);
        long e = elapsed(unit);
        if (s >= Long.MAX_VALUE - e) {
            return Long.MAX_VALUE;
        } else {
            return s + e;
        }
    }

    @Override
    public boolean expired() {
        return elapsed(TeaseLib.DURATION_TIME_UNIT) >= limit;
    }

}
