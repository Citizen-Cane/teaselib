package teaselib;

import java.util.concurrent.TimeUnit;

public interface State {
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    Options apply();

    Options applyTo(Object... items);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    State remove();

    State removeFrom(Object... peers);

    interface Options {
        State over(long duration, TimeUnit unit);

        State over(Duration duration);
    }
}
