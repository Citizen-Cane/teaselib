package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    public final String name;
    public final int id;
    public final int fromElement;
    public final int toElement;

    public final List<Rule> children;
    public final float probability;
    public final Confidence confidence;

    public Rule(String name, int id, int fromElement, int toElement, float probability, Confidence confidence) {
        super();
        this.name = name;
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
                + confidence + " children=" + children.size();
    }

}
