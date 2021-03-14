package teaselib.core.speechrecognition;

public class Hypothesis extends Rule {

    public static final String Name = "Hypothesis";

    public Hypothesis(Rule rule) {
        this(rule, rule.probability);
    }

    public Hypothesis(Rule rule, float probability) {
        super(rule, Name, probability, Confidence.valueOf(probability));
    }

}
