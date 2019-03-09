package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    public final String name;
    public final String text;
    public final int id;
    public final int fromElement;
    public final int toElement;

    public final List<Rule> children;
    public final float probability;
    public final Confidence confidence;

    public Rule(String name, String text, int id, int fromElement, int toElement, float probability,
            Confidence confidence) {
        super();
        this.name = name;
        this.text = text;
        this.id = id;
        this.fromElement = fromElement;
        this.toElement = toElement;
        this.children = new ArrayList<>();
        this.probability = probability;
        this.confidence = confidence;
    }

    public void add(Rule rule) {
        children.add(rule);
    }

    @Override
    public String toString() {
        return "Name=" + name + " Id=" + id + " [" + fromElement + "," + toElement + "[ C=" + probability + "~"
                + confidence + " children=" + children.size() + " \"" + text + "\"";
    }

    public String prettyPrint() {
        return prettyPrint(new StringBuilder(), this, 0).toString();
    }

    private static StringBuilder prettyPrint(StringBuilder rules, Rule rule, int indention) {
        for (int i = 0; i < indention; ++i) {
            rules.append("\t");
        }
        rules.append(rule);
        if (!rule.children.isEmpty()) {
            rules.append("\n");
            rule.children.stream().forEach(child -> prettyPrint(rules, child, indention + 1));
        }
        return rules;
    }

}
