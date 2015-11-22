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
        TeaseLib.instance().log.info("Script interrupted at");
        for (StackTraceElement ste : getStackTrace()) {
            TeaseLib.instance().log.info("\t" + ste.toString());
        }
    }
}
