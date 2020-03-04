package teaselib.core.speechrecognition.events;

import java.util.Arrays;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.Rule;

public class SpeechRecognizedEventArgs extends SpeechRecognitionEventArgs {
    public final Rule[] result;

    public SpeechRecognizedEventArgs(Rule... result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Result = "
                + Arrays.stream(result).map(Rule::prettyPrint).collect(Collectors.joining("\n"));
    }
}
