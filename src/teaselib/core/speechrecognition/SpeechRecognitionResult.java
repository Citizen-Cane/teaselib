package teaselib.core.speechrecognition;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

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
        return "#" + rule.id + ": " + text + " (" + rule.probability + ") -> confidence="
                + rule.confidence.toString().toLowerCase();
    }

    public String prettyPrint() {
        return prettyPrint(new StringBuilder(), rule, 0).toString();
    }

    private StringBuilder prettyPrint(StringBuilder rules, Rule rule, int indention) {
        for (int i = 0; i < indention; ++i) {
            rules.append("\t");
        }
        rules.append(getText(rule));
        rules.append(" -> ");
        rules.append(rule);
        if (!rule.children.isEmpty()) {
            rules.append("\n");
            rule.children.stream().forEach(child -> prettyPrint(rules, child, indention + 1));
        }
        return rules;
    }

}
