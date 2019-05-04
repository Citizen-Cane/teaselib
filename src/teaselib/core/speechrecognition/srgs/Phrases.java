package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import teaselib.core.ui.Choices;

public class Phrases extends ArrayList<Rule> {
    private static final long serialVersionUID = 1L;

    public static final int COMMON_RULE = Integer.MIN_VALUE;

    public static Rule rule(int group, int ruleIndex, String... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static Rule rule(int group, int ruleIndex, OneOf... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static OneOf oneOf(int choiceIndex, String item) {
        return new OneOf(choiceIndex, item);
    }

    public static OneOf oneOf(int choiceIndex, String... items) {
        return new OneOf(choiceIndex, items);
    }

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> choices) {
        // TODO More sophisticated approach
        return SimplifiedPhrases.of(choices);
    }

    public static Phrases of(Choices choices) {
        // TODO More sophisticated approach
        return SimplifiedPhrases.of(choices);
    }

    Phrases() {
    }

    public Sequences<String> flatten() {
        // TODO Should be 0 if common rule but isn't - blocks other code
        int choices = choices();
        StringSequences flattened = StringSequences.ignoreCase(choices);
        for (int i = 0; i < choices; i++) {
            StringSequence sequence = StringSequence.ignoreCase();
            flattened.add(sequence);
        }

        int rules = rules();
        for (int choiceIndex = 0; choiceIndex < choices; choiceIndex++) {
            for (int ruleIndex = 0; ruleIndex < rules; ruleIndex++) {
                for (Rule rule : this) {
                    String word = "";
                    if (rule.index == ruleIndex) {
                        OneOf items = rule.get(0);
                        if (rule.size() == 1 && items.size() == 1) {
                            if (items.choiceIndex == choiceIndex || items.choiceIndex == COMMON_RULE) {
                                word = items.iterator().next();
                            }
                        } else {
                            for (OneOf item : rule) {
                                if (item.choiceIndex == choiceIndex) {
                                    // Flatten can only flat first phrase
                                    word = item.iterator().next();
                                    break;
                                }
                            }
                        }
                        flattened.get(choiceIndex).add(word);
                    }
                }
            }
        }
        return flattened;
    }

    Rule previous(Rule rule) {
        return get(rule.group, rule.index - 1);
    }

    Rule next(Rule rule) {
        return get(rule.group, rule.index + 1);
    }

    private Rule get(int group, int index) {
        for (Rule rule : this) {
            if (rule.index == index && rule.group == group) {
                return rule;
            }
        }
        throw new NoSuchElementException("group = " + group + ", index = " + index);
    }

    public int groups() {
        Optional<Rule> reduced = stream().reduce((a, b) -> {
            return a.group > b.group ? a : b;
        });
        return reduced.isPresent() ? reduced.get().group + 1 : 1;
    }

    public int choices() {
        Optional<Rule> reduced = stream().reduce((a, b) -> {
            return a.choices() > b.choices() ? a : b;
        });
        return reduced.isPresent() ? reduced.get().choices() : 1;
    }

    public int rules() {
        Optional<Rule> reduced = stream().reduce((a, b) -> {
            return a.index > b.index ? a : b;
        });
        return reduced.isPresent() ? reduced.get().index + 1 : 1;
    }

}
