package teaselib.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.util.DurationFormat;

public class FrozenDuration extends AbstractDuration {

    private final long elapsed;

    public FrozenDuration(long start, long limit, long elapsed, TimeUnit unit) {
        super(convertToDuration(start, unit), convertToDuration(limit, unit));
        this.elapsed = convertToDuration(elapsed, unit);
    }

    public FrozenDuration(Duration duration) {
        super(duration.start(TeaseLib.DURATION_TIME_UNIT), duration.limit(TeaseLib.DURATION_TIME_UNIT));
        this.elapsed = duration.elapsed(TeaseLib.DURATION_TIME_UNIT);
    }

    @Override
    public long elapsed(TimeUnit unit) {
        return convertToUnit(elapsed, unit);
    }

    @Override
    public long remaining(TimeUnit unit) {
        return convertToUnit(start - elapsed + limit, unit);
    }

    @Override
    public String toString() {
        return new Date(start(TimeUnit.MILLISECONDS))
                + (limit > 0 ? "+" + DurationFormat.toString(limit, TeaseLib.DURATION_TIME_UNIT) : "");
    }

}
