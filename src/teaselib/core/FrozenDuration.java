package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.util.DurationFormat;

/**
 * A duration that has ended.
 * 
 * @author Citizen-Cane
 *
 */
public class FrozenDuration extends AbstractDuration {

    private final long elapsed;

    public FrozenDuration(TeaseLib teaseLib, long start, long limit, TimeUnit unit) {
        this(teaseLib, start, limit, 0, unit);
    }

    public FrozenDuration(TeaseLib teaseLib, long start, long limit, long elapsed, TimeUnit unit) {
        super(teaseLib, convertToDuration(start, unit), convertToDuration(limit, unit));
        this.elapsed = convertToDuration(elapsed, unit);
    }

    public FrozenDuration(TeaseLib teaseLib, Duration duration) {
        super(teaseLib, duration.start(TeaseLib.DURATION_TIME_UNIT), duration.limit(TeaseLib.DURATION_TIME_UNIT));
        this.elapsed = duration.elapsed(TeaseLib.DURATION_TIME_UNIT);
    }

    @Override
    public long elapsed(TimeUnit unit) {
        return convertToUnit(elapsed, unit);
    }

    @Override
    public long since(TimeUnit unit) {
        if (start == 0) {
            return Long.MAX_VALUE;
        } else {
            return convertToUnit(teaseLib.getTime(TeaseLib.DURATION_TIME_UNIT) - start - elapsed, unit);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (elapsed ^ (elapsed >>> 32));
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
        FrozenDuration other = (FrozenDuration) obj;
        return elapsed == other.elapsed;
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS))
                + (limit > 0 ? "+" + DurationFormat.toString(limit, TeaseLib.DURATION_TIME_UNIT) : "");
    }

}
