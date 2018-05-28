package teaselib;

import java.util.concurrent.TimeUnit;

public interface State {
    public static final long REMOVED = -1;
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    Options apply();

    @SuppressWarnings("unchecked")
    <S extends Object> Options applyTo(S... items);

    boolean is(Object... attributes);

    boolean applied();

    boolean expired();

    Duration duration();

    Persistence remove();

    @SuppressWarnings("unchecked")
    <S extends Object> Persistence removeFrom(S... peers);

    interface Options extends State.Persistence {
        Persistence over(long duration, TimeUnit unit);

        Persistence over(Duration duration);
    }

    interface Persistence {
        void remember();
    }
}
