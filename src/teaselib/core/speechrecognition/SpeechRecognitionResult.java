package teaselib.core.speechrecognition;

import java.util.List;

public class SpeechRecognitionResult {

    public static final int UNKNOWN_PHRASE_INDEX = -1;

    public final int index;
    public final String text;
    public final Rule rule;
    public final float probability;
    public final Confidence confidence;

    public SpeechRecognitionResult(int index, String text, Rule rule, float propability, Confidence confidence) {
        super();
        this.index = index;
        this.text = text;
        this.rule = rule;
        this.probability = propability;
        this.confidence = confidence;
    }

    public boolean isChoice(List<String> choices) {
        return UNKNOWN_PHRASE_INDEX < index && index < choices.size();
    }

    public boolean hasHigherProbabilityThan(SpeechRecognitionResult recognitionResult) {
        return probability > recognitionResult.probability
                || confidence.probability > recognitionResult.confidence.probability;
    }

    @Override
    public String toString() {
        return "#" + index + ": " + text + " (" + probability + ") -> confidence="
                + confidence.toString().toLowerCase();
    }
}
