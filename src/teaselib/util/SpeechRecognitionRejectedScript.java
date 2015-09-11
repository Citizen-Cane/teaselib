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

    @Override
    final protected String showChoices(ScriptFunction scriptFunction,
            List<String> choices, Confidence recognitionConfidence) {
        // Always use low confidence for recognitions
        return super.showChoices(scriptFunction, choices, confidence);
    }

}
