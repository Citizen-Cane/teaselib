package teaselib.core.speechrecognition;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public interface PreparedChoices extends Consumer<SpeechRecognitionImplementation> {

    public static final IntUnaryOperator IdentityMapping = index -> index;

    @Override
    void accept(SpeechRecognitionImplementation sri);

    default Optional<Rule> hypothesis(List<Rule> rules, Rule currentHypothesis) {
        return Optional.empty();
    }

    IntUnaryOperator mapper();
}