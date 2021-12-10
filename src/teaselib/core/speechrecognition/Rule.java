package teaselib.core.speechrecognition;

import static java.util.Collections.*;
import static java.util.function.Predicate.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.speechrecognition.srgs.PhraseString;
import teaselib.core.speechrecognition.srgs.Sequence;
import teaselib.core.speechrecognition.srgs.SlicedPhrases;

/**
 * @author Citizen-Cane
 *
 */
public class Rule {

    public static final String MAIN_RULE_NAME = "Recognized";
    public static final int MAIN_RULE_INDEX = -1;
    public static final String REPAIRED_MAIN_RULE_NAME = "Repaired";
    public static final String WITHOUT_IGNOREABLE_TRAILING_NULL_RULE = "Trailing Null rule removed";
    public static final String CHOICE_NODE_NAME = "r";

    public static final Rule Timeout = new Rule("Timeout", "", Integer.MIN_VALUE, emptyList(), 0, 0,
            Confidence.Definite.probability);
    public static final Rule Nothing = new Rule("Nothing", "", Integer.MIN_VALUE, emptyList(), 0, 0,
            Confidence.Noise.probability);
    public static final Rule Noise = new Rule("Noise", "", Integer.MIN_VALUE, emptyList(), 0, 0,
            Confidence.Noise.probability);

    public static final Set<Integer> NoIndices = emptySet();

    public final String name;
    public final String text;
    public final int ruleIndex;
    public final Set<Integer> indices;
    public final int fromElement;
    public final int toElement;

    public final List<Rule> children;
    public final float probability;

    public static Rule mainRule(List<Rule> children) {
        if (children.isEmpty()) {
            return Rule.Nothing;
        } else {
            return mainRule(children, probability(children));
        }
    }

    private static Rule mainRule(List<Rule> children, float probability) {
        String name = name(MAIN_RULE_NAME, MAIN_RULE_INDEX, children);
        String text = text(children);
        int fromElement = children.get(0).fromElement;
        int toElement = children.get(children.size() - 1).toElement;
        return new Rule(name, text, MAIN_RULE_INDEX, children, fromElement, toElement, probability);
    }

    public static Rule placeholder(int index, int position, int choice, float probability) {
        return placeholder(index, position, Collections.singleton(choice), probability);
    }

    public static Rule placeholder(int index, int position, Set<Integer> choices, float probability) {
        return new Rule(name(CHOICE_NODE_NAME, index, choices), "", position, choices, position, position, probability);
    }

    public static String name(String name, int index, List<Rule> children) {
        return name(name, index, indicesIntersection(children));
    }

    public static String name(String name, int index, Set<Integer> choices) {
        var s = new StringBuilder(name);
        if (index > MAIN_RULE_INDEX) {
            s.append("_");
            s.append(index);
        }
        if (!choices.isEmpty()) {
            s.append("_");
            s.append(toString(choices));
        }
        return s.toString();
    }

    private static String toString(Set<Integer> choices) {
        return choices.stream().map(Objects::toString).collect(Collectors.joining(","));
    }

    public Rule(Rule rule, float probability) {
        this(rule.name, rule.text, rule.ruleIndex, rule.indices, rule.children, rule.fromElement, rule.toElement,
                probability);
    }

    public Rule(Rule rule, String name, float probability) {
        this(name, rule.text, rule.ruleIndex, rule.indices, rule.children, rule.fromElement, rule.toElement,
                probability);
    }

    public Rule(String name, String text, int ruleIndex, Set<Integer> indices, int fromElement, int toElement,
            float probability) {
        this(name, text, ruleIndex, indices, new ArrayList<>(), fromElement, toElement, probability);
    }

    public Rule(String name, List<Rule> children) {
        this(name, text(children), -1, children, children.get(0).fromElement,
                children.get(children.size() - 1).toElement, probability(children));
    }

    public Rule(String name, String text, int ruleIndex, List<Rule> children, int fromElement, int toElement,
            float probability) {
        this(name, text, ruleIndex, indicesIntersection(children), children, fromElement, toElement, probability);
    }

    public Rule(String name, List<String> children, int choice, float probability) {
        this(name, children.stream().collect(joining(" ")), -1, singleton(choice),
                childRules(children, choice, probability), 0, children.size(), probability);
    }

    private static List<Rule> childRules(List<String> children, int choice, float probability) {
        List<Rule> rules = new ArrayList<>(children.size());
        for (int i = 0; i < children.size(); ++i) {
            rules.add(new Rule(Rule.CHOICE_NODE_NAME + "_" + i + "_" + choice, children.get(i), i, singleton(choice), i,
                    i + 1, probability));
        }
        return rules;
    }

    private Rule(String name, String text, int ruleIndex, Set<Integer> indices, List<Rule> children, int fromElement,
            int toElement, float probability) {
        Precoditions.checkProbability(probability);

        this.name = name;
        this.text = text;
        this.ruleIndex = ruleIndex;
        this.indices = indices;
        this.fromElement = fromElement;
        this.toElement = toElement;
        this.children = unmodifiableList(children);
        this.probability = probability;
    }

    public static Rule maxProbability(Rule a, Rule b) {
        if (a.probability == b.probability) {
            return a.text.length() > b.text.length() ? a : b;
        } else {
            return a.probability > b.probability ? a : b;
        }
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
                + "[ C=" + probability + "~" + Confidence.valueOf(probability) + " children=" + children.size() + " \""
                + text + "\"";
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

    static final class Precoditions {
        public static void checkProbability(float value) {
            if (value < 0.0f || value > 1.0)
                throw new IllegalArgumentException("Probability must be in [0,1]: " + value);
        }
    }

    public class IllegalRuleException extends IllegalStateException {
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

            boolean equalsIgnoreCase = (children.isEmpty()
                    ? words.subList(fromElement, toElement).stream().collect(Collectors.joining(" "))
                    : text(children)).equalsIgnoreCase(text);
            if (!equalsIgnoreCase) {
                throw new IllegalRuleException(mainRule, "Rule text must match child rules");
            } else {
                return children.stream().allMatch(child -> child.isValid(mainRule));
            }
        }
    }

    public boolean hasIgnoreableTrailingNullRule() {
        if (children.isEmpty()) {
            return false;
        } else {
            Rule child = children.get(children.size() - 1);
            return child.text == null && child.fromElement > PhraseString.words(text).length;
        }
    }

    public Rule withoutIgnoreableTrailingNullRules() {
        List<Rule> childrenWithoutTrailingNullRule = children.stream()
                .filter(child -> child.fromElement <= PhraseString.words(text).length).collect(toList());
        return new Rule(WITHOUT_IGNOREABLE_TRAILING_NULL_RULE, text, ruleIndex, childrenWithoutTrailingNullRule,
                fromElement, toElement, probability);
    }

    public boolean hasTrailingNullRule() {
        if (children.isEmpty()) {
            return false;
        } else {
            Rule child = children.get(children.size() - 1);
            return child.text == null && child.fromElement >= PhraseString.words(text).length;
        }
    }

    public Rule withoutDisjunctTrailingNullRules(IntUnaryOperator toChoices) {
        if (children.isEmpty()) {
            throw new NoSuchElementException(text);
        }
        Rule lastChild = children.get(children.size() - 1);
        if (lastChild.text != null) {
            throw new IllegalStateException(lastChild.toString());
        }

        List<Rule> childrenWithoutTrailingNullRule = new ArrayList<>(children.subList(0, children.size() - 1));
        Set<Integer> newIndices = RuleIndicesList.intersection(
                childrenWithoutTrailingNullRule.stream().map(rule -> rule.indices).collect(Collectors.toList()));
        Set<Integer> choices = newIndices.stream().map(toChoices::applyAsInt).collect(Collectors.toSet());
        if (choices.size() != 1) {
            childrenWithoutTrailingNullRule.add(lastChild);
        }

        return new Rule(WITHOUT_IGNOREABLE_TRAILING_NULL_RULE, text, ruleIndex, childrenWithoutTrailingNullRule,
                fromElement, toElement - lastChild.fromElement, probability);
    }

    public Set<Integer> intersectionWithoutNullRules() {
        return new RuleIndicesList(children.stream().filter(r -> r.text != null).map(r -> r.indices).collect(toList()))
                .intersection();
    }

    private List<Integer> repairableNullRules(SlicedPhrases<PhraseString> slicedPhrases) {
        int slices = slicedPhrases.size();
        List<Integer> repairableNullRules = new ArrayList<>();
        int i = 0;
        for (Rule child : children) {
            if (child.text == null && //
                    !isSpuriousTrailingNullRule(child, slices)) {
                repairableNullRules.add(i);
            }
            i++;
        }
        return repairableNullRules;

    }

    private static boolean isSpuriousTrailingNullRule(Rule rule, int slices) {
        return rule.ruleIndex < 0 || rule.ruleIndex >= slices;
    }

    public List<Rule> repair(SlicedPhrases<PhraseString> slicedPhrases) {
        Set<Integer> intersection = intersectionWithoutNullRules();
        List<Rule> candidates = new ArrayList<>();
        if (intersection.size() != 1)
            return candidates;

        boolean lastRuleRepaired = false;
        for (Integer index : repairableNullRules(slicedPhrases)) {
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
        var nullRule = children.get(index);
        int elementOffset = replacement.words().size();

        List<Rule> repairedChildren = new ArrayList<>(children.subList(0, index.intValue()));
        repairedChildren.add(new Rule(nullRule.name, replacement.phrase, nullRule.ruleIndex, replacement.indices,
                nullRule.fromElement, nullRule.toElement + elementOffset, probability));

        for (int k = index.intValue() + 1; k < children.size(); k++) {
            Rule r = children.get(k);
            repairedChildren.add(new Rule(r.name, r.text, r.ruleIndex, r.indices, r.fromElement + elementOffset,
                    r.toElement + elementOffset, r.probability));
        }

        return new Rule(REPAIRED_MAIN_RULE_NAME, text(repairedChildren), ruleIndex, repairedChildren, fromElement,
                toElement + elementOffset, probability);
    }

    public static float probability(List<Rule> children) {
        return children.stream().flatMap(rule -> {
            if (rule.text == null) {
                // production data shows NULL rules as C=1.0, but they don't account to main rule probability
                return Stream.empty();
            } else {
                String[] words = PhraseString.words(rule.text);
                if (words.length == 0) {
                    // placeholder rule
                    return Stream.of(rule);
                } else {
                    return Arrays.stream(words).map(word -> rule);
                }
            }
        }).collect(Collectors.averagingDouble(child -> child.probability)).floatValue();
    }

    public static String text(List<Rule> children) {
        return children.stream().map(r -> r.text) //
                .filter(Objects::nonNull).filter(not(String::isBlank)).collect(joining(" "));
    }

    private static Set<Integer> indicesIntersection(List<Rule> rules) {
        return RuleIndicesList.of(rules).intersection();
    }

}
