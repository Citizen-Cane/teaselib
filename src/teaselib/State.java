package teaselib;

import java.util.concurrent.TimeUnit;

import teaselib.core.util.ReflectionUtils;

public interface State {
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    public static String Available = ReflectionUtils.qualified(State.class, "Available");
    public static String Unavailable = ReflectionUtils.qualified(State.class, "Unavailable");

    Options apply();

    Options applyTo(Object... peers);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    void remove();

    void removeFrom(Object... peers);

    interface Options {
        State over(long duration, TimeUnit unit);

        State over(Duration duration);
    }
}
