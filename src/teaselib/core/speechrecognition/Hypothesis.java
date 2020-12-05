package teaselib.core.speechrecognition;

import static java.util.Collections.emptyList;

public class Hypothesis extends Rule {

    public static final String Name = "Hypothesis";

    public static final Hypothesis None = new Hypothesis(
            new Rule("None", "", Integer.MIN_VALUE, emptyList(), 0, 0, 0.0f, Confidence.Noise));

    public Hypothesis(Rule rule) {
        this(rule, rule.probability);
    }

    public Hypothesis(Rule rule, float probability) {
        super(rule, Name, probability, rule.confidence);
    }

}
