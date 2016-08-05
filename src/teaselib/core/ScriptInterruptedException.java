package teaselib.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author someone
 *
 *         Implements script interruption. Must be thrown if a script gets
 *         interrupted. Used to cancel script closures upon choice.
 */
public class ScriptInterruptedException extends RuntimeException {
    private static final Logger logger = LoggerFactory
            .getLogger(ScriptInterruptedException.class);

    private static final long serialVersionUID = 1L;

    public ScriptInterruptedException() {
        final StackTraceElement[] stackTrace = getStackTrace();
        if (stackTrace.length > 0) {
            final StackTraceElement firstElement = stackTrace[0];
            logger.info("interrupted at " + firstElement.toString());
            for (StackTraceElement element : stackTrace) {
                if (element != firstElement) {
                    logger.debug("\t" + element.toString());
                }
            }
        }
    }
}
