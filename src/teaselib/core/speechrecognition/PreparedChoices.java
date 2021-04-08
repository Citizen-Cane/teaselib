package teaselib.core.speechrecognition;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public interface PreparedChoices extends Consumer<SpeechRecognitionImplementation> {

    public static final IntUnaryOperator IdentityMapping = index -> index;

    @Override
    void accept(SpeechRecognitionImplementation sri);

    public float hypothesisWeight(Rule rule);

    IntUnaryOperator mapper();
}
