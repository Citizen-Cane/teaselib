package teaselib;

import java.util.concurrent.TimeUnit;

import teaselib.core.TeaseLib.Duration;

public interface State {

    Duration getDuration();

    boolean valid();

    boolean expired();

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

    void apply();

    /**
     * Start a duration on the item. This clears any previous durations.
     * 
     * @param time
     * @param unit
     */
    void apply(long time, TimeUnit unit);
}
