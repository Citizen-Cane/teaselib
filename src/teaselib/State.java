package teaselib;

import java.util.concurrent.TimeUnit;

import teaselib.core.TeaseLib.Duration;

// TODO Clean up so this can be understood
// -> expected() -> duration(), getDuration() -> getElapsed(), and so on
/**
 * A states defines a temporary or indefinite relationship between two objects.
 * 
 * @author Citizen-Cane
 *
 */
public interface State {
    static final Long INFINITE = Long.MAX_VALUE;
    static final Long REMOVED = Long.MIN_VALUE;

    // TODO Find better name - used if the state just applies time, no item
    static final String None = new String("None");
    static final String Timed = new String("Timed");

    /**
     * The duration of this state.
     * 
     * @return
     */
    Duration getDuration();

    /**
     * Whether the state is expired.
     * 
     * @return
     */
    boolean expired();

    /**
     * Duration of the state.
     * 
     * @return
     */
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
    State remember();

    /**
     * Remove the item. The start time is set to the current time when calling
     * the function, and the duration is set to {@link State#REMOVED}.
     * <p>
     * Because the state won't be persisted anymore, but still cached, scripts
     * can still react on it as long as the current main script is running.
     */
    State remove();

    /**
     * Apply the item, but don't set a expiration duration.
     * <p>
     * The state is set active but expired, so {@link State#remaining} returns
     * 0, {@link State#expired} returns true, and {@link State#applied} returns
     * true until the item is removed.
     */
    <T> State apply(T what);

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
    <T> State apply(T what, long time, TimeUnit unit);

    /**
     * The item the state is applied to.
     * 
     * @return
     */
    public <T> T item();

    /**
     * The object applied to the item.
     * 
     * @return
     */
    public <T> T what();
}
