package teaselib.core.speechrecognition.events;

import teaselib.core.speechrecognition.SpeechRecognitionResult;

public class SpeechRecognizedEventArgs extends SpeechRecognitionEventArgs {
    public final SpeechRecognitionResult result[];

    public SpeechRecognizedEventArgs(SpeechRecognitionResult... result) {
        this.result = result;
    }

    @Override
    public String toString() {
        StringBuilder resultString = new StringBuilder();
        if (result != null) {
            for (SpeechRecognitionResult r : result) {
                if (resultString.length() > 0) {
                    resultString.append(", ");
                }
                resultString.append(r.toString());
            }
        } else {
            resultString.append("<none>");
        }
        return getClass().getSimpleName() + "Result = " + resultString;
    }
}
