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
    final List<Rule> children;
    public final float probability;
    public final Confidence confidence;

    public Rule(String name, int id, float probability, Confidence confidence) {
        super();
        this.name = name;
        this.id = id;
        this.children = new ArrayList<>();
        this.probability = probability;
        this.confidence = confidence;
    }

    public void add(Rule rule) {
        children.add(rule);
    }
}
