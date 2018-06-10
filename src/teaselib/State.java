package teaselib;

import java.util.concurrent.TimeUnit;

public interface State {
    public static final long REMOVED = -1;
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    Options apply();

    Options applyTo(Object... items);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    Persistence remove();

    Persistence removeFrom(Object... peers);

    interface Options extends State.Persistence {
        Persistence over(long duration, TimeUnit unit);

        Persistence over(Duration duration);
    }

    interface Persistence {
        void remember();
    }
}
