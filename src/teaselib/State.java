package teaselib;

import java.util.concurrent.TimeUnit;

public interface State {
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    Options apply();

    Options applyTo(Object... peers);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    void remove();

    void removeFrom(Object... peers);

    interface Options extends State.Persistence {
        Persistence over(long duration, TimeUnit unit);

        Persistence over(Duration duration);
    }

    interface Persistence {
        public enum Until {
            Removed,
            Expired
        }

        void remember(Until forget);
    }
}
