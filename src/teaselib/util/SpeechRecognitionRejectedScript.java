package teaselib.util;

import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.functional.RunnableScript;

/**
 * @author Citizen-Cane
 *
 */
public abstract class SpeechRecognitionRejectedScript extends TeaseScript implements RunnableScript {

    public SpeechRecognitionRejectedScript(Script script) {
        super(script);
    }

    /**
     * Override to determine when the script should run. This could be done inside the script by just returning without
     * doing anything at all, but would result in flickering in the ui, because the buttons get unrealized, the
     * immediately realized again.
     * 
     * @return Whether to run the script.
     */
    public boolean canRun() {
        return true;
    }

}
