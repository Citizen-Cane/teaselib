package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.util.DurationFormat;

public class FrozenDuration extends AbstractDuration {

    private final long elapsed;

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
        return teaseLib.getTime(unit) - convertToUnit(start + elapsed, unit);
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS))
                + (limit > 0 ? "+" + DurationFormat.toString(limit, TeaseLib.DURATION_TIME_UNIT) : "");
    }

}
