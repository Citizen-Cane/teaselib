package teaselib.core.state;

import java.util.concurrent.TimeUnit;

import teaselib.Duration;
import teaselib.core.ItemLogger;
import teaselib.core.TimeOfDay;

public class DurationProxy implements Duration {

    private final String owner;
    private final Duration duration;
    private final ItemLogger logger;

    public DurationProxy(String owner, Duration duration, ItemLogger logger) {
        this.owner = owner;
        this.duration = duration;
        this.logger = logger;
    }

    @Override
    public long start(TimeUnit unit) {
        long start = duration.start(unit);
        logger.log(owner, "start", start, unit);
        return start;
    }

    @Override
    public TimeOfDay start() {
        TimeOfDay start = duration.start();
        logger.log(owner, "start", start.toString());
        return start;
    }

    @Override
    public long limit(TimeUnit unit) {
        long limit = duration.limit(unit);
        logger.log(owner, "limit", limit, unit);
        return limit;
    }

    @Override
    public TimeOfDay limit() {
        TimeOfDay limit = duration.limit();
        logger.log(owner, "limit", limit.toString());
        return limit;
    }

    @Override
    public long elapsed(TimeUnit unit) {
        long elapsed = duration.elapsed(unit);
        logger.log(owner, "elapsed", elapsed, unit);
        return elapsed;
    }

    @Override
    public long remaining(TimeUnit unit) {
        long remaining = duration.remaining(unit);
        logger.log(owner, "remaining", remaining, unit);
        return remaining;
    }

    @Override
    public boolean expired() {
        boolean expired = duration.expired();
        logger.log(owner, "expired", expired);
        return expired;
    }

    @Override
    public long end(TimeUnit unit) {
        long end = duration.end(unit);
        logger.log(owner, "end", end, unit);
        return end;
    }

    @Override
    public TimeOfDay end() {
        TimeOfDay end = duration.end();
        logger.log(owner, "end", end.toString());
        return end;
    }

    @Override
    public long since(TimeUnit unit) {
        return duration.since(unit);
    }

}
