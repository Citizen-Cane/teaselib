/**
 * 
 */
package teaselib.util;

import java.util.List;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.core.TeaseScriptBase;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;

/**
 * @author someone
 *
 */
public abstract class SpeechRecognitionRejectedScript extends TeaseScript {

    private final Confidence confidence;

    public SpeechRecognitionRejectedScript(TeaseScriptBase script) {
        super(script);
        confidence = Confidence.Low;
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

    @Override
    final protected String showChoices(ScriptFunction scriptFunction, Confidence recognitionConfidence,
            List<String> choices) {
        // Always use low confidence for recognitions
        return super.showChoices(scriptFunction, confidence, choices);
    }

}
