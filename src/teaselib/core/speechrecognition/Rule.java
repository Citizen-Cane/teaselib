package teaselib.core.speechrecognition;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.Sequence;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    public static final String MAIN_RULE_NAME = "Recognized";
    public static final String HYPOTHESIS = "Hypothesis";
    public static final String REPAIRED_MAIN__RULE_NAME = "Repaired";
    public static final String WITHOUT_IGNOREABLE_TRAILING_NULL_RULE = "Trailing Null rule removed";
    public static final String CHOICE_NODE_PREFIX = "r_";

    public static final Set<Integer> NoIndices = Collections.emptySet();

    public final String name;
    public final String text;
    public final int ruleIndex;
    public final Set<Integer> indices;
    public final int fromElement;
    public final int toElement;

    public final List<Rule> children;
    public final float probability;
    public final Confidence confidence;

    public Rule(Rule rule, String name, float probability, Confidence confidence) {
        this(name, rule.text, rule.ruleIndex, rule.indices, rule.children, rule.fromElement, rule.toElement,
                probability, confidence);
    }

    public Rule(String name, String text, int ruleIndex, Set<Integer> indices, int fromElement, int toElement,
            float probability, Confidence confidence) {
        this(name, text, ruleIndex, indices, new ArrayList<>(), fromElement, toElement, probability, confidence);
    }

    public Rule(String name, String text, int ruleIndex, List<Rule> children, int fromElement, int toElement,
            float probability, Confidence confidence) {
        this(name, text, ruleIndex, indicesIntersection(children), children, fromElement, toElement, probability,
                confidence);
    }

    private Rule(String name, String text, int ruleIndex, Set<Integer> indices, List<Rule> children, int fromElement,
            int toElement, float probability, Confidence confidence) {
        this.name = name;
        this.text = text;
        this.ruleIndex = ruleIndex;
        this.indices = indices;
        this.fromElement = fromElement;
        this.toElement = toElement;
        this.children = Collections.unmodifiableList(children);
        this.probability = probability;
        this.confidence = confidence;
    }

    public static Rule maxProbability(Rule a, Rule b) {
        return a.probability > b.probability ? a : b;
    }

    public RuleIndicesList indices() {
        if (children.isEmpty()) {
            return RuleIndicesList.singleton(indices);
        } else {
            return RuleIndicesList.of(children);
        }
    }

    @Override
    public String toString() {
        String displayedRuleIndex = ruleIndex == Integer.MIN_VALUE ? "" : " ruleIndex=" + ruleIndex;
        return "Name=" + name + displayedRuleIndex + " choices=" + indices + " [" + fromElement + "," + toElement
                + "[ C=" + probability + "~" + confidence + " children=" + children.size() + " \"" + text + "\"";
    }

    public String prettyPrint() {
        return prettyPrint(new StringBuilder(), this, 0).toString();
    }

    private static StringBuilder prettyPrint(StringBuilder rules, Rule rule, int indention) {
        rules.append("\n");

        for (int i = 0; i < indention; ++i) {
            rules.append("\t");
        }
        rules.append(rule);

        if (!rule.children.isEmpty()) {
            rule.children.stream().forEach(child -> prettyPrint(rules, child, indention + 1));
        }
        return rules;
    }

    public boolean isValid() {
        return isValid(this);
    }

    class IllegalRuleException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        final Rule rule;

        public IllegalRuleException(Rule rule, String message) {
            super(message + ": " + rule.prettyPrint());
            this.rule = rule;
        }

    }

    private boolean isValid(Rule mainRule) {
        if (fromElement == toElement && text == null) { // null rule
            return true;
        } else {
            List<String> words = Arrays.asList(PhraseString.words(mainRule.text));
            if (fromElement > words.size()) {
                throw new IllegalRuleException(mainRule, "Rule contains less words than fromElement");
            }
            if (toElement > words.size()) {
                throw new IllegalRuleException(mainRule, "Rule contains less words than toElement");
            }

            List<String> elements = words.subList(fromElement, toElement);
            boolean equalsIgnoreCase = elements.stream().collect(joining(" ")).equalsIgnoreCase(text);
            if (!equalsIgnoreCase) {
                throw new IllegalRuleException(mainRule, "Rule text must match child rules");
            } else {
                return children.stream().allMatch(child -> child.isValid(mainRule));
            }
        }
    }

    public boolean hasTrailingNullRule() {
        Rule child = children.get(children.size() - 1);
        return child.text == null && child.fromElement > PhraseString.words(text).length;
    }

    public Rule withoutIgnoreableTrailingNullRules() {
        List<Rule> childrenWithoutTrailingNullRule = children.stream()
                .filter(child -> child.fromElement <= PhraseString.words(text).length).collect(toList());
        return new Rule(WITHOUT_IGNOREABLE_TRAILING_NULL_RULE, text, ruleIndex, childrenWithoutTrailingNullRule,
                fromElement, toElement, probability, confidence);
    }

    public Set<Integer> intersectionWithoutNullRules() {
        return new RuleIndicesList(children.stream().filter(r -> r.text != null).map(r -> r.indices).collect(toList()))
                .intersection();
    }

    private List<Integer> repairableNullRules(int slices) {
        List<Integer> repairableNullRules = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Rule child = children.get(i);
            if (child.text == null) {
                if (!isSpuriousTrailingNullRule(child, slices)) {
                    repairableNullRules.add(i);
                }
            }
        }
        return repairableNullRules;
    }

    private static boolean isSpuriousTrailingNullRule(Rule rule, int slices) {
        return rule.ruleIndex < 0 || rule.ruleIndex >= slices;
    }

    public List<Rule> repair(SlicedPhrases<PhraseString> slicedPhrases) {
        Set<Integer> intersection = intersectionWithoutNullRules();
        List<Rule> candidates = new ArrayList<>();
        boolean lastRuleRepaired = false;
        for (Integer index : repairableNullRules(slicedPhrases.size())) {
            for (Sequence<PhraseString> sequence : slicedPhrases.get(index)) {
                PhraseString replacement = sequence.joined();
                if (PhraseString.intersect(replacement.indices, intersection)) {
                    Rule repaired = repair(index, replacement);
                    List<Rule> repairedSuccessors = repaired.repair(slicedPhrases);
                    if (repairedSuccessors.isEmpty()) {
                        candidates.add(repaired);
                    } else {
                        candidates.addAll(repairedSuccessors);
                        lastRuleRepaired = true;
                    }
                }
            }
            if (lastRuleRepaired)
                break;
        }
        return candidates;
    }

    private Rule repair(Integer index, PhraseString replacement) {
        Rule nullRule = children.get(index);
        int elementOffset = replacement.words().size();

        List<Rule> repairedChildren = new ArrayList<>(children.subList(0, index.intValue()));
        repairedChildren.add(new Rule(nullRule.name, replacement.phrase, nullRule.ruleIndex, replacement.indices,
                nullRule.fromElement, nullRule.toElement + elementOffset, probability, confidence));

        for (int k = index.intValue() + 1; k < children.size(); k++) {
            Rule r = children.get(k);
            repairedChildren.add(new Rule(r.name, r.text, r.ruleIndex, r.indices, r.fromElement + elementOffset,
                    r.toElement + elementOffset, r.probability, r.confidence));
        }

        return new Rule(REPAIRED_MAIN__RULE_NAME, text(repairedChildren), ruleIndex, repairedChildren, fromElement,
                toElement + elementOffset, probability, confidence);
    }

    private static String text(List<Rule> repairedChildren) {
        return repairedChildren.stream().map(r -> r.text).filter(Objects::nonNull).collect(joining(" "));
    }

    private static Set<Integer> indicesIntersection(List<Rule> rules) {
        return RuleIndicesList.of(rules).intersection();
    }

}
