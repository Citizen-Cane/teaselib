package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import teaselib.core.ui.Choices;
import teaselib.util.math.Partition;

public class Phrases extends ArrayList<Rule> {
    private static final long serialVersionUID = 1L;

    public static final int COMMON_RULE = Integer.MIN_VALUE;

    final int choiceCount;

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
        return SimplifiedPhrases.of(choices);
    }

    public static Phrases of(Choices choices) {
        return SimplifiedPhrases.of(choices);
    }

    Phrases(int choiceCount) {
        this.choiceCount = choiceCount;
    }

    public Sequences<String> flatten() {
        int choices = choices();
        Sequences<String> flattened = StringSequences.of(choices);
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
        return get(rule.group, rule.index - 1).orElseThrow();
    }

    Rule next(Rule rule) {
        return get(rule.group, rule.index + 1).orElseThrow();
    }

    private Optional<Rule> get(int group, int index) {
        for (Rule rule : this) {
            if (rule.index == index && rule.group == group) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
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

    public static Phrases ofSliced(Choices choices) {
        List<ChoiceString> all = choices.stream().flatMap(
                choice -> choice.phrases.stream().map(phrase -> new ChoiceString(phrase, choices.indexOf(choice))))
                .collect(Collectors.toList());
        Partition<ChoiceString> groups = new Partition<>(all, Phrases::haveCommonParts);

        // phrases are grouped by common slices
        // + For each group, get the head, slice and group by common parts -> recursion
        // + if groups don't contain any common parts, emit rules for that part

        Phrases phrases = new Phrases(choices.size());

        int groupIndex = 0;
        for (Partition<ChoiceString>.Group group : groups) {
            recurse(phrases, Collections.singletonList(group), groupIndex++, 0);
        }

        return phrases;
    }

    // TODO Improve performance by providing sliced choice strings as input (saves us from rebuilding strings)
    private static void recurse(Phrases phrases, List<Partition<ChoiceString>.Group> groups, int groupIndex,
            int ruleIndex) {
        for (Partition<ChoiceString>.Group group : groups) {
            List<Sequences<ChoiceString>> sliced = ChoiceStringSequences.slice(group.items);

            // As in simplified phrases, a common part with optional choice parts on both sides
            // must be joined with both sides -> produces single phrase rule, correct but not optimal
            // TODO Keep the common part common - turn this into groups or optimize
            // TODO Model this with rules - group index can't be used here right now, maybe need to change the data
            // model
            if (!sliced.isEmpty()) {
                Sequences<ChoiceString> first = sliced.remove(0);
                // Join if this or next contain empty slices
                if (!sliced.isEmpty() && !phrases.allChoicesDefined(groupIndex)) {
                    Sequences<ChoiceString> second = sliced.get(0);
                    // before common part
                    // too greedy -> test optional parts on both sides per phrase? - would conflict with original
                    // intention
                    // TODO find out when to join, and provide a clear set of rules
                    if (first.containsOptionalParts() || second.containsOptionalParts()) {
                        first = first.joinWith(second);
                        sliced.remove(0);
                        if (!sliced.isEmpty()) {
                            // After common
                            second = sliced.get(0);
                            if (second.containsOptionalParts()) {
                                first = first.joinWith(second);
                                sliced.remove(0);
                            }
                        }
                    }
                }

                if (isCommon(first)) {
                    OneOf items = new OneOf(Phrases.COMMON_RULE,
                            first.stream().map(sequence -> sequence.join(ChoiceString::concat).phrase).distinct()
                                    .collect(Collectors.toList()));
                    phrases.add(new Rule(groupIndex, ruleIndex, items));
                } else {
                    Function<? super ChoiceString, ? extends Integer> classifier = phrase -> phrase.choice;
                    Function<? super ChoiceString, ? extends String> mapper = phrase -> phrase.phrase;
                    Map<Integer, List<String>> items = first.stream().filter(sequence -> !sequence.isEmpty())
                            .map(phrase -> phrase.join(ChoiceString::concat)).collect(Collectors.groupingBy(classifier,
                                    HashMap::new, Collectors.mapping(mapper, Collectors.toList())));

                    Optional<Rule> optional = phrases.get(groupIndex, ruleIndex);
                    Rule rule = optional.isPresent() ? optional.get() : new Rule(groupIndex, ruleIndex);
                    items.entrySet().stream().forEach(entry -> rule.add(new OneOf(entry.getKey(), entry.getValue())));
                    if (optional.isEmpty()) {
                        phrases.add(rule);
                    }
                }

                // TODO continue with rest of slice -> rebuild sequence instead of strings
                if (!sliced.isEmpty()) {
                    ruleIndex++;
                    List<ChoiceString> flattened = Sequences.flatten(sliced, first.equalsOperator,
                            ChoiceString::concat);
                    recurse(phrases, new Partition<>(flattened, Phrases::haveCommonParts).groups, groupIndex,
                            ruleIndex);
                }
            }
        }
    }

    private boolean allChoicesDefined(int groupIndex) {
        int ruleIndex = 0;
        while (true) {
            Optional<Rule> rule = get(groupIndex, ruleIndex++);
            if (rule.isEmpty()) {
                return false;
            } else if (rule.get().choices() == choiceCount) {
                return true;
            }
        }
    }

    private static boolean isCommon(Sequences<ChoiceString> slice) {
        return isAlreadyCommon(slice) || !differentChoices(slice);
    }

    private static boolean isAlreadyCommon(Sequences<ChoiceString> slice) {
        return slice.size() == 1 && slice.get(0).get(0).choice == Phrases.COMMON_RULE;
    }

    private static boolean differentChoices(Sequences<ChoiceString> slice) {
        Map<String, Integer> unique = new HashMap<>();
        for (Sequence<ChoiceString> sequence : slice) {
            if (!sequence.isEmpty()) {
                ChoiceString words = sequence.join(ChoiceString::concat);
                Integer choice = unique.get(words.phrase);
                if (choice != null && choice != words.choice) {
                    return false;
                }
                unique.put(words.phrase, words.choice);
            }
        }
        return true;
    }

    static boolean haveCommonParts(ChoiceString a, ChoiceString b) {
        List<Sequences<String>> slice = StringSequences.of(a.toString(), b.toString());
        return slice.stream().anyMatch(sequence -> sequence.size() == 1);
    }

}
