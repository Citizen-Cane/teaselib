package teaselib.core.speechrecognition;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public interface PreparedChoices extends Consumer<SpeechRecognitionProvider> {

    public static final IntUnaryOperator IdentityMapping = index -> index;

    @Override
    void accept(SpeechRecognitionProvider sri);

    public float weightedProbability(Rule rule);

    IntUnaryOperator mapper();
}
