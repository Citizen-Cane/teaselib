package teaselib.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen-Cane
 *
 *         Implements script interruption. Must be thrown if a script gets interrupted. Used to cancel script closures
 *         upon choice.
 *         <p>
 *         This is to be used if a function is in tended to be called by the script directly. Otherwise it's a good idea
 *         to avoid the additional dependency and stick to the standard {@link java.lang.InterruptedException}.
 */
public class ScriptInterruptedException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(ScriptInterruptedException.class);

    private static final long serialVersionUID = 1L;

    public ScriptInterruptedException(InterruptedException e) {
        super(e);
    }

    public ScriptInterruptedException() {
        StackTraceElement[] stackTrace = this.getStackTrace();
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
