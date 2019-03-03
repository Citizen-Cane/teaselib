package teaselib.core.speechrecognition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    final String name;
    final int id;
    final int fromElement;
    final int toElement;

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
        return name + " ruleid=" + id + " -> (" + fromElement + "," + toElement + ") @" + probability + "~" + confidence
                + " children" + children;
    }

}
