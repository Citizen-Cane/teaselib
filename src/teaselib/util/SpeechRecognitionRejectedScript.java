/**
 * 
 */
package teaselib.util;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.ui.Choices;

/**
 * @author someone
 *
 */
public abstract class SpeechRecognitionRejectedScript extends TeaseScript {

    private final Confidence confidence;

    public SpeechRecognitionRejectedScript(Script script) {
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
    protected String showChoices(Choices choices, ScriptFunction scriptFunction, Confidence recognitionConfidence) {
        // Always use low confidence for recognitions
        return super.showChoices(choices, scriptFunction, confidence);
    }

}
