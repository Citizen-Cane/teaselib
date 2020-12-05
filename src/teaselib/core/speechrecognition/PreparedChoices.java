package teaselib.core.speechrecognition;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public interface PreparedChoices extends Consumer<SpeechRecognitionProvider> {

    public static final IntUnaryOperator IdentityMapping = index -> index;

    @Override
    void accept(SpeechRecognitionProvider sri);

    default Hypothesis improve(Hypothesis hypothesis, List<Rule> rules) {
        return new Hypothesis(rules.get(0));
    }

    IntUnaryOperator mapper();
}
