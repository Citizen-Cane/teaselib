package teaselib;

import java.util.concurrent.TimeUnit;

import teaselib.core.TeaseLib.Duration;

// TODO Clean up so this can be understood
// -> expected() -> duration(), getDuration() -> getElapsed(), and so on
public interface State {
    static final Long SESSION = Long.MAX_VALUE;
    static final Long INFINITE = Long.MAX_VALUE - 1;

    Duration getDuration();

    boolean valid();

    boolean expired();

    long expected();

    /**
     * Time until the item expires.
     * 
     * @param unit
     *            THe unit of the return value.
     * @return The remaining time for the item.
     */
    long remaining(TimeUnit unit);

    /**
     * @return Whether the item is currently applied.
     * 
     */
    boolean applied();

    boolean freeSince(long time, TimeUnit unit);

    /**
     * Remove the item.
     */
    void remove();

    /**
     * Apply the item, but assume it's removed anytime before the end of the
     * session, e.g. before the current main script terminates.
     * <p>
     * The state is set active but expired, so {@link State#remaining} returns
     * 0, {@link State#expired} returns true, and {@link State#applied} returns
     * true until the item is removed.
     * <p>
     * Furthermore the state is not persisted beyond the scope of the main
     * script.
     */
    void applyForSession();

    /**
     * Apply the item, but don't set a expiration duration. Use with care
     * because this might interfere with other scripts if never reset.
     * <p>
     * The state is set active but expired, so {@link State#remaining} returns
     * 0, {@link State#expired} returns true, and {@link State#applied} returns
     * true until the item is removed.
     * <p>
     * Unlike {@link State#applyForSession} the state is persisted and available
     * until removed.
     */
    void applyIndefinitely();

    /**
     * Start a duration on the item. This clears any previous durations.
     * 
     * @param time
     * @param unit
     */
    void apply(long time, TimeUnit unit);
}
