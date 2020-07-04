package teaselib;

import teaselib.functional.CallableScript;
import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public class ScriptFunction {
    /**
     * Return string to state that the script function has timed out.
     */
    public static final String TimeoutString = "Timeout";

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

    public static class AnswerOverride extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public final Answer answer;

        public AnswerOverride(Answer answer) {
            this.answer = answer;
        }

    }

    private final CallableScript<Answer> script;
    /**
     * The type of this script function;
     */
    public final Relation relation;

    public ScriptFunction(RunnableScript script) {
        this(script, Relation.Autonomous);
    }

    public ScriptFunction(RunnableScript script, Relation relation) {
        this(() -> {
            script.run();
            return null;
        }, relation);
    }

    public ScriptFunction(CallableScript<Answer> script) {
        this(script, Relation.Autonomous);
    }

    public ScriptFunction(CallableScript<Answer> script, Relation relation) {
        this.script = script;
        this.relation = relation;
    }

    public Answer call() {
        return script.call();
    }
}
