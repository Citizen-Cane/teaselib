package teaselib.core;

/**
 * @author someone
 *
 * Implements script interruption. Must be thrown if a script gets interrupted. Used to cancel script closures upon choice.
 */
public class ScriptInterruptedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
}
