package teaselib.core.speechrecognition;

import java.util.List;

public class SpeechRecognitionResult {

    public static final int UNKNOWN_PHRASE_INDEX = -1;

    public final String text;
    public final Rule rule;

    public SpeechRecognitionResult(String text, Rule rule) {
        this.text = text;
        this.rule = rule;
    }

    public boolean isChoice(List<String> choices) {
        return UNKNOWN_PHRASE_INDEX < rule.id && rule.id < choices.size();
    }

    public boolean hasHigherProbabilityThan(SpeechRecognitionResult recognitionResult) {
        return rule.probability > recognitionResult.rule.probability
                || rule.confidence.probability > recognitionResult.rule.confidence.probability;
    }

    @Override
    public String toString() {
        return "#" + rule.id + ": " + text + " (" + rule.probability + ") -> confidence="
                + rule.confidence.toString().toLowerCase();
    }
}
