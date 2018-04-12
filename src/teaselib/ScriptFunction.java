/**
 * 
 */
package teaselib;

import teaselib.functional.CallableScript;
import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptFunction {
    /**
     * Return value to state that the script function has timed out.
     */
    public static final String Timeout = "Timeout";

    /**
     * To sleep infinitely.
     */
    public static final long Infinite = Long.MAX_VALUE;

    /**
     * Defines the relation between the script function and the last message.
     *
     */
    public enum Relation {
        /**
         * Specifies that the script function begins a new message, and doesn't relate to the current message.
         */
        Autonomous,
        /**
         * Specifies that the script function completes the current message.
         */
        Confirmation
    }

    CallableScript<String> script;
    /**
     * The type of this script function;
     */
    public final Relation relation;

    /**
     * The result of the script function. Initialized with the default value to state that the function just ended.
     * Setting the result causes that value to be returned by {@link TeaseScript#reply}.
     */
    private String result = null;

    public ScriptFunction(RunnableScript script) {
        this(script, Relation.Autonomous);
    }

    public ScriptFunction(RunnableScript script, Relation relation) {
        this(() -> {
            script.run();
            return Timeout;
        }, relation);
    }

    public ScriptFunction(CallableScript<String> script) {
        this(script, Relation.Autonomous);
    }

    public ScriptFunction(CallableScript<String> script, Relation relation) {
        this.script = script;
        this.relation = relation;
    }

    public String call() {
        return script.call();
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
