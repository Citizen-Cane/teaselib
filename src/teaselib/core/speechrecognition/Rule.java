package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.srgs.Phrases;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    public final String name;
    public final String text;
    public final int ruleIndex;
    public final int choiceIndex;
    public final int fromElement;
    public final int toElement;

    public final List<Rule> children;
    public final float probability;
    public final Confidence confidence;

    public Rule(Rule rule, Confidence confidence) {
        this(rule, confidence.probability, confidence);
    }

    public Rule(Rule rule, float probability, Confidence confidence) {
        this(rule.name, rule.text, rule.ruleIndex, rule.choiceIndex, rule.fromElement, rule.toElement, probability,
                confidence);
    }

    public Rule(String name, String text, int ruleIndex, int choiceInddex, int fromElement, int toElement,
            float probability, Confidence confidence) {
        this.name = name;
        this.text = text;
        this.ruleIndex = ruleIndex;
        this.choiceIndex = choiceInddex;
        this.fromElement = fromElement;
        this.toElement = toElement;
        this.children = new ArrayList<>();
        this.probability = probability;
        this.confidence = confidence;
    }

    public void add(Rule rule) {
        children.add(rule);
    }

    public Rule withChoiceProbability() {
        List<Rule> childrenWithChoices = children.stream().filter(child -> child.choiceIndex != Phrases.COMMON_RULE)
                .collect(Collectors.toList());
        float average = (float) childrenWithChoices.stream().mapToDouble(child -> child.probability).average()
                .orElse(0.0f);

        return new Rule(this, probability, Confidence.valueOf(average));
    }

    public boolean hasHigherProbabilityThan(Rule rule) {
        return probability > rule.probability || confidence.probability > rule.confidence.probability;
    }

    public static Rule maxProbability(Rule a, Rule b) {
        return a.probability > b.probability ? a : b;
    }

    @Override
    public String toString() {
        String choice = choiceIndex == Phrases.COMMON_RULE ? " Common" : (" choiceIndex=" + choiceIndex);
        return "Name=" + name + " sequenceIndex=" + ruleIndex + choice + " [" + fromElement + "," + toElement + "[ C="
                + probability + "~" + confidence + " children=" + children.size() + " \"" + text + "\"";
    }

    public String prettyPrint() {
        return prettyPrint(new StringBuilder(), this, 0).toString();
    }

    private static StringBuilder prettyPrint(StringBuilder rules, Rule rule, int indention) {
        for (int i = 0; i < indention; ++i) {
            rules.append("\t");
        }
        rules.append(rule);
        rules.append("\n");

        if (!rule.children.isEmpty()) {
            rule.children.stream().forEach(child -> prettyPrint(rules, child, indention + 1));
        }
        return rules;
    }

}
