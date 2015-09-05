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
     * Defines the relation between the script function and the last message
     *
     */
    public enum Relation {
        /**
         * Specifies that the script function begins a new message, and doesn't
         * relate to the current message.
         */
        Autonomous,
        /**
         * Specifies that the script function completes the current message.
         */
        Confirmation
    }

    /**
     * The type of this script function;
     */
    public final Relation relation;

    /**
     * The result of the script function. Initialized with the default value to
     * state that the function just ended.
     */
    public String result = Finished;

    public ScriptFunction() {
        this.relation = Relation.Autonomous;
    }

    public ScriptFunction(Relation relation) {
        this.relation = relation;
    }
}
