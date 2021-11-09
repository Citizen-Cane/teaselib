package teaselib.core.speechrecognition.events;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.Rule;

public class SpeechRecognizedEventArgs extends SpeechRecognitionEventArgs {
    public final List<Rule> result;

    public SpeechRecognizedEventArgs(Rule result) {
        Objects.requireNonNull(result);
        this.result = Collections.singletonList(result);
    }

    public SpeechRecognizedEventArgs(List<Rule> result) {
        Objects.requireNonNull(result);
        this.result = Collections.unmodifiableList(result);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " Result = "
                + result.stream().map(Rule::prettyPrint).collect(Collectors.joining(""));
    }

}
