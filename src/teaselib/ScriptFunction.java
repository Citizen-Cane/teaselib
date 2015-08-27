/**
 * 
 */
package teaselib;

/**
 * @author someone
 *
 */
public abstract class ScriptFunction implements Runnable {

    /**
     * Return value to state that the script function has finished normally.
     */
    public static final String Finished = "Finished";

    /**
     * Return value to state that the script function timed out.
     */
    public static final String Timeout = "Timeout";

    /**
     * To sleep infinitely.
     */
    public static final long Infinite = Long.MAX_VALUE;

    /**
     * The result of the script function. Initialized with the default value to
     * state that the function just ended.
     */
    public String result = Finished;
}
