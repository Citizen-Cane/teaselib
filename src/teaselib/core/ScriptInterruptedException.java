package teaselib.core;

import teaselib.TeaseLib;

/**
 * @author someone
 *
 *         Implements script interruption. Must be thrown if a script gets
 *         interrupted. Used to cancel script closures upon choice.
 */
public class ScriptInterruptedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ScriptInterruptedException() {
        final StackTraceElement[] stackTrace = getStackTrace();
        if (stackTrace.length > 0) {
            final StackTraceElement firstElement = stackTrace[0];
            TeaseLib.instance().log
                    .info("interrupted at " + firstElement.toString());
            for (StackTraceElement element : stackTrace) {
                if (element != firstElement) {
                    TeaseLib.instance().log.debug("\t" + element.toString());
                }
            }
        }
    }
}
