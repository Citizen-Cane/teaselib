package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

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

    public String getText(Rule rule) {
        // TODO List sequence
        List<String> elements = Arrays.asList(text.split(" "));
        StringJoiner stringJoiner = new StringJoiner(" ");
        for (String element : elements.subList(rule.fromElement, rule.toElement)) {
            stringJoiner.add(element);
        }
        return stringJoiner.toString();
    }

    @Override
    public String toString() {
        return "#" + index + ": " + text + " (" + probability + ") -> confidence="
                + confidence.toString().toLowerCase();
    }
}
