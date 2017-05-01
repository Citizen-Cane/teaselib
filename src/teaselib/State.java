package teaselib;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface State {
    public static final long REMOVED = -1;
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    <S extends Object> State.Options apply(S... items);

    Set<Object> peers();

    boolean is(Object object);

    boolean applied();

    boolean expired();

    Duration duration();

    <S extends Object> State remove();

    <S extends Object> State remove(S items);

    interface Options extends State.Persistence {
        Persistence over(long duration, TimeUnit unit);

        Persistence over(Duration duration);
    }

    interface Persistence {
        State remember();
    }
}
