/**
 * 
 */
package teaselib.util;

import java.util.List;

import teaselib.ScriptFunction;
import teaselib.TeaseScript;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;

/**
 * @author someone
 *
 */
public abstract class SpeechRecognitionRejectedScript extends TeaseScript {

    private final Confidence confidence;

    public SpeechRecognitionRejectedScript(TeaseScript script) {
        super(script);
        confidence = Confidence.Low;
    }

    /**
     * Override to determine when the script should run. This could be done
     * inside the script by just returning without doing anything at all, but
     * would result in flickering in the ui, because the buttons get unrealized,
     * the immediately realized again.
     * 
     * @return Whether to run the script.
     */
    public abstract boolean canRun();

    @Override
    final protected String showChoices(ScriptFunction scriptFunction,
            List<String> choices, Confidence recognitionConfidence) {
        // Always use low confidence for recognitions
        return super.showChoices(scriptFunction, choices, confidence);
    }

}
