package teaselib;

import java.util.concurrent.TimeUnit;

public interface State {
    public static final long TEMPORARY = 0;

    /**
     * Applies the state.
     * 
     * @return Duration-{@link Options} for this state.
     */
    Options apply();

    /**
     * Apply the state to peers.
     * 
     * @param peers
     *            The peers to apply this state to.
     * @return Duration-{@link Options} for this item.
     */
    Options applyTo(Object... peers);

    boolean is(Object... attributes);

    /**
     * Whether the state is applied.
     * 
     * @return True if applied.
     */
    boolean applied();

    boolean expired();

    Duration duration();

    void remove();

    void removeFrom(Object... peers);

    boolean removed();

    long removed(TimeUnit unit);

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

    public interface Attributes {
        void applyAttributes(Object... attributes);
    }
}
