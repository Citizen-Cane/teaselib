package teaselib;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface State {
    public static final long REMOVED = -1;
    public static final long TEMPORARY = 0;

    public static final long INDEFINITELY = Long.MAX_VALUE;

    Set<Object> peers();

    // TODO add apply() in order to make item and state more similar
    // TODO rename to applyTo()
    <S extends Object> State.Options apply(S... items);

    boolean is(Object... objects);

    boolean applied();

    boolean expired();

    Duration duration();

    State remove();

    State remove(Object peer);

    interface Options extends State.Persistence {
        Persistence over(long duration, TimeUnit unit);

        Persistence over(Duration duration);
    }

    interface Persistence {
        State remember();
    }
}
