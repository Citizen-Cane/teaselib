package teaselib;

import java.util.concurrent.TimeUnit;

import teaselib.core.TeaseLib.Duration;

// TODO Clean up so this can be understood
// -> expected() -> duration(), getDuration() -> getElapsed(), and so on
public interface State {
    static final Long INFINITE = Long.MAX_VALUE;
    static final Long REMOVED = Long.MIN_VALUE;

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

    /**
     * Whether the item has not been applied before the given time.
     * <p>
     * TODO Better name, does it do what's intended
     * 
     * @param time
     * @param unit
     * @return
     */
    boolean freeSince(long time, TimeUnit unit);

    /**
     * Make the state persistent.
     */
    void remember();

    /**
     * Remove the item. The start time is set to the current time when calling
     * the function, and the duration is set to {@link State#REMOVED}.
     * <p>
     * Because the state won't be persisted anymore, but still cached, scripts
     * can still react on it as long as the current main script is running.
     */
    void remove();

    /**
     * Apply the item, but don't set a expiration duration.
     * <p>
     * The state is set active but expired, so {@link State#remaining} returns
     * 0, {@link State#expired} returns true, and {@link State#applied} returns
     * true until the item is removed.
     */
    void apply();

    /**
     * Start a new duration of the item.
     * <p>
     * You may specify the duration as {@link State#INFINITE} to indicate that
     * the item shouldn't be removed. However the consequence would be that you
     * are solely responsible for resetting the state.
     * 
     * @param time
     * @param unit
     */
    void apply(long time, TimeUnit unit);
}
