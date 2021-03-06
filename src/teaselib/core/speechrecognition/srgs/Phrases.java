package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
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

    public static OneOf oneOf(int choiceIndex, String... items) {
        return new OneOf(Collections.singletonList(choiceIndex), Arrays.asList(items));
    }

    public static OneOf oneOf(List<Integer> choices, String... items) {
        return new OneOf(choices, Arrays.asList(items));
    }

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> choices) {
        return Phrases.of(new Choices(choices.stream().map(Choice::new).collect(Collectors.toList())));
    }

    Phrases(int choiceCount) {
        this.choiceCount = choiceCount;
    }

    Phrases(int choiceCount, List<Rule> rules) {
        this.choiceCount = choiceCount;
        addAll(rules);
    }

    /**
     * Flattens phrases to input strings.
     * 
     * @return A list containing the first phrase of each choice.
     */
    public Sequences<String> flatten() {
        int choices = choices();
        Sequences<String> flattened = StringSequences.of(choices);
        for (int i = 0; i < choices; i++) {
            StringSequence sequence = StringSequence.ignoreCase();
            flattened.add(sequence);
        }

        int rules = rules();
        int groups = groups();
        Set<Integer> processed = new HashSet<>();

        for (int group = 0; group < groups; group++) {
            for (int choiceIndex = 0; choiceIndex < choices; choiceIndex++) {
                if (!processed.contains(choiceIndex)) {
                    boolean choiceProcessed = false;
                    for (int ruleIndex = 0; ruleIndex < rules; ruleIndex++) {
                        for (Rule rule : this) {
                            String word = "";
                            if (rule.group == group && rule.index == ruleIndex) {
                                for (OneOf items : rule) {
                                    // The sequence of the items in OneOf matters
                                    if (items.choices.contains(choiceIndex) || items.choices.contains(COMMON_RULE)) {
                                        word = items.iterator().next();
                                        choiceProcessed = true;
                                        break;
                                    }
                                }
                                flattened.get(choiceIndex).add(word);
                            }
                        }
                    }
                    if (choiceProcessed) {
                        processed.add(choiceIndex);
                    }
                }
            }
        }
        return flattened;
    }

    Optional<Rule> get(int group, int index) {
        for (Rule rule : this) {
            if (rule.index == index && rule.group == group) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    int groups() {
        Optional<Rule> reduced = stream().reduce((a, b) -> {
            return a.group > b.group ? a : b;
        });
        return reduced.isPresent() ? reduced.get().group + 1 : 1;
    }

    int choices() {
        return choiceCount;
    }

    int rules() {
        Optional<Rule> reduced = stream().reduce((a, b) -> {
            return a.index > b.index ? a : b;
        });
        return reduced.isPresent() ? reduced.get().index + 1 : 1;
    }

    public static Phrases of(Choices choices) {
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
            recurse(phrases, Collections.singletonList(group.items), groupIndex++, 0);
        }

        // remove completely empty rules that might have been added as a result of processing optional parts
        // + blank OneOf elements are suppressed later on by the SRGSBuilder
        return new Phrases(phrases.choiceCount,
                phrases.stream().filter(rule -> !rule.isBlank()).collect(Collectors.toList()));
    }

    // TODO Improve performance by providing sliced choice strings as input (saves us from rebuilding strings)
    private static void recurse(Phrases phrases, List<List<ChoiceString>> groups, int groupIndex, int ruleIndex) {
        for (List<ChoiceString> group : groups) {
            List<Sequences<ChoiceString>> sliced = ChoiceStringSequences.slice(group);

            if (!sliced.isEmpty()) {
                Sequences<ChoiceString> first = sliced.remove(0);
                // Join if this or next contain empty slices
                if (!sliced.isEmpty() && !phrases.allChoicesDefined(groupIndex)) {
                    Sequences<ChoiceString> second = sliced.get(0);
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

                if (isAlreadyCommon(first)) {
                    List<Integer> choices = group.stream().map(p -> p.choice).distinct().collect(toList());
                    List<ChoiceString> elements = first.stream().map(first.joinSequenceOperator::apply).distinct()
                            .collect(toList());
                    List<String> strings = elements.stream().map(s -> s.phrase).distinct().collect(toList());
                    OneOf items = new OneOf(choices, strings);
                    Rule rule = phrases.rule(groupIndex, ruleIndex);
                    rule.add(items);
                } else if (!differentChoices(first)) {
                    // TODO Create OneOf items for each different choice
                    // - this should create a common(n,m) for each distinct phrase part
                    // -> TODO map multiple phrase parts to multiple choices -> create test cases
                    Function<? super ChoiceString, ? extends String> classifier = phrase -> phrase.phrase;
                    Function<? super ChoiceString, ? extends Integer> mapper = phrase -> phrase.choice;
                    Map<String, List<Integer>> items = first.stream().filter(sequence -> !sequence.isEmpty())
                            .map(first.joinSequenceOperator::apply)
                            .collect(groupingBy(classifier, HashMap::new, mapping(mapper, toList())));

                    Rule rule = phrases.rule(groupIndex, ruleIndex);
                    items.entrySet().stream().forEach(
                            entry -> rule.add(new OneOf(entry.getValue(), Collections.singletonList(entry.getKey()))));
                    // TODO Find out how this can supersede the else branch
                } else {
                    Function<? super ChoiceString, ? extends Integer> classifier = phrase -> phrase.choice;
                    Function<? super ChoiceString, ? extends String> mapper = phrase -> phrase.phrase;
                    Map<Integer, List<String>> items = first.stream().filter(sequence -> !sequence.isEmpty())
                            .map(first.joinSequenceOperator::apply)
                            .collect(groupingBy(classifier, HashMap::new, mapping(mapper, toList())));

                    Rule rule = phrases.rule(groupIndex, ruleIndex);
                    items.entrySet().stream().forEach(entry -> rule
                            .add(new OneOf(Collections.singletonList(entry.getKey()), distinct(entry.getValue()))));
                }

                if (!sliced.isEmpty()) {
                    ruleIndex++;
                    // TODO continue with rest of slice -> rebuild sequence instead of strings
                    List<ChoiceString> flattened = Sequences.flatten(sliced, first.equalsOperator,
                            first.joinSequenceOperator);
                    recurse(phrases, Collections.singletonList(flattened), groupIndex, ruleIndex);
                }
            }
        }
    }

    private Rule rule(int groupIndex, int ruleIndex) {
        Optional<Rule> rule = get(groupIndex, ruleIndex);
        if (!rule.isPresent()) {
            rule = Optional.of(new Rule(groupIndex, ruleIndex));
            add(rule.get());
        }
        return rule.get();
    }

    private static List<String> distinct(List<String> items) {
        return items.stream().distinct().collect(Collectors.toList());
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

    private static boolean isAlreadyCommon(Sequences<ChoiceString> slice) {
        return slice.size() == 1 && slice.get(0).get(0).choice == Phrases.COMMON_RULE;
    }

    private static boolean differentChoices(Sequences<ChoiceString> slice) {
        Map<String, Integer> unique = new HashMap<>();
        for (Sequence<ChoiceString> sequence : slice) {
            if (!sequence.isEmpty()) {
                ChoiceString words = slice.joinSequenceOperator.apply(sequence);
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
