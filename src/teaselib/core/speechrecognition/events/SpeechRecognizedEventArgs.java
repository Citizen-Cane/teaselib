package teaselib.core.speechrecognition.events;

import teaselib.core.speechrecognition.Rule;

public class SpeechRecognizedEventArgs extends SpeechRecognitionEventArgs {
    public final Rule result[];

    public SpeechRecognizedEventArgs(Rule... result) {
        this.result = result;
    }

    @Override
    public String toString() {
        StringBuilder resultString = new StringBuilder();
        if (result != null) {
            for (Rule rule : result) {
                if (resultString.length() > 0) {
                    resultString.append(", ");
                }
                resultString.append(rule.toString());
            }
        } else {
            resultString.append("<none>");
        }
        return getClass().getSimpleName() + "Result = " + resultString;
    }
}
