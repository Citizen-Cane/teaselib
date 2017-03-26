package teaselib;

import java.util.concurrent.TimeUnit;

public interface Duration {
    static final long INFINITE = Long.MAX_VALUE;

    long start(TimeUnit unit);

    long limit(TimeUnit unit);

    long elapsed(TimeUnit unit);

    long remaining(TimeUnit unit);

    boolean expired();
}