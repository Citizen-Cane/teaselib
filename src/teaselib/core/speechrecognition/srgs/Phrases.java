package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private final int choiceCount;

    public static Rule rule(int group, int ruleIndex, String... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static Rule rule(int group, int ruleIndex, OneOf... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static OneOf oneOf(int choiceIndex, String... items) {
        return new OneOf(Collections.singleton(choiceIndex), Arrays.asList(items));
    }

    public static OneOf oneOf(Set<Integer> choices, String... items) {
        return new OneOf(choices, Arrays.asList(items));
    }

    static Phrases of(String... choices) {
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

    private Optional<Rule> get(int group, int index) {
        for (Rule rule : this) {
            if (rule.index == index && rule.group == group) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public int groups() {
        Optional<Rule> reduced = stream().reduce((a, b) -> a.group > b.group ? a : b);
        return reduced.isPresent() ? reduced.get().group + 1 : 1;
    }

    public int choices() {
        return choiceCount;
    }

    public int rules() {
        Optional<Rule> reduced = stream().reduce((a, b) -> a.index > b.index ? a : b);
        return reduced.isPresent() ? reduced.get().index + 1 : 1;
    }

    public static Phrases of(Choices choices) {
        IndexMap<Integer> index2choices = new IndexMap<>();

        // TODO Decide about OneOf elements:
        // - should phrase chunks be exchangeable like before - multiple phrases share the same choice index
        // -> allows flexible speaking and automatic combination of phrase chunks
        // + (yes miss of course, miss)
        // -> possibly still need groups to avoid mixing phrases that have nothing in common

        // - or should phrases be more exact - each phrase has its own index -> later lookup choice index
        // -> allow each phrase to be spoken exact (no mixing), but also no wrong phrases
        // + ("Yes Miss of Course", "Of course Miss")
        // -> groups can be removed because slicing would now reduce them to a disjunct partition

        // TODO Find out if there is a way to have both

        List<PhraseString> all = choices.stream()
                .flatMap(choice -> choice.phrases.stream()
                        .map(phrase -> new PhraseString(phrase, index2choices.add(choices.indexOf(choice)))))
                .collect(toList());

        // List<PhraseString> all = choices.stream().flatMap(
        // choice -> choice.phrases.stream().map(phrase -> new PhraseString(phrase, choices.indexOf(choice))))
        // .collect(toList());

        Partition<PhraseString> groups = new Partition<>(all, Phrases::haveCommonParts);

        // phrases are grouped by common slices
        // + For each group, get the head, slice and group by common parts -> recursion
        // + if groups don't contain any common parts, emit rules for that part

        Phrases phrases = new Phrases(choices.size());

        int groupIndex = 0;
        for (Partition<PhraseString>.Group group : groups) {
            recurse(phrases, Collections.singletonList(group.items), groupIndex++, 0, index2choices);
        }

        // remove completely empty rules that might have been added as a result of processing optional parts
        // + blank OneOf elements are suppressed later on by the SRGSBuilder
        return new Phrases(phrases.choiceCount,
                phrases.stream().filter(rule -> !rule.isBlank()).collect(Collectors.toList()));

        // TODO Refactor phrase creation into constructor
    }

    // TODO Improve performance by providing sliced choice strings as input (saves us from rebuilding strings)
    private static void recurse(Phrases phrases, List<List<PhraseString>> groups, int groupIndex, int ruleIndex,
            IndexMap<Integer> index2choices) {
        for (List<PhraseString> group : groups) {
            List<Sequences<PhraseString>> sliced = PhraseStringSequences.slice(group);

            while (!sliced.isEmpty()) {
                Sequences<PhraseString> first = sliced.remove(0);

                // // Join if this or next contain empty slices
                // TODO Can be removed when srgs reports "" as a result rule
                if (!sliced.isEmpty() && !phrases.allChoicesDefined(groupIndex)) {
                    Sequences<PhraseString> second = sliced.get(0);
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

                Function<? super PhraseString, ? extends Set<Integer>> classifier = phrase -> index2choices
                        .get(phrase.indices);
                // Function<? super PhraseString, ? extends Set<Integer>> classifier = phrase -> phrase.indices;
                Function<? super PhraseString, ? extends String> mapper = phrase -> phrase.phrase;
                Map<Set<Integer>, List<String>> items = first.stream().filter(sequence -> !sequence.isEmpty())
                        .map(first.traits.joinSequenceOperator::apply)
                        .collect(groupingBy(classifier, LinkedHashMap::new, mapping(mapper, toList())));

                Rule rule = phrases.rule(groupIndex, ruleIndex);
                items.entrySet().stream()
                        .forEach(entry -> rule.add(new OneOf(entry.getKey(), distinct(entry.getValue()))));

                ruleIndex++;
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

    static boolean haveCommonParts(PhraseString a, PhraseString b) {
        List<Sequences<String>> slice = StringSequences.of(a.toString(), b.toString());
        return slice.stream().anyMatch(sequence -> sequence.size() == 1);
    }

}
