package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.util.math.Partition;

public class Phrases extends ArrayList<Phrases.Rule> {
    private static final long serialVersionUID = 1L;

    public static final int COMMON_RULE = Integer.MIN_VALUE;

    public static Rule rule(int group, int ruleIndex, String... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static Rule rule(int group, int ruleIndex, OneOf... items) {
        return new Rule(group, ruleIndex, items);
    }

    public static OneOf oneOf(int choiceIndex, String item) {
        return new Phrases.OneOf(choiceIndex, item);
    }

    public static OneOf oneOf(int choiceIndex, String... items) {
        return new Phrases.OneOf(choiceIndex, items);
    }

    public static class OneOf extends HashSet<String> {
        private static final long serialVersionUID = 1L;

        final int choiceIndex;

        public OneOf(int choiceIndex) {
            this.choiceIndex = choiceIndex;
        }

        public OneOf(int choiceIndex, int capacity) {
            super(capacity);
            this.choiceIndex = choiceIndex;
        }

        public OneOf(int choiceIndex, String item) {
            this(choiceIndex, Collections.singletonList(item));
        }

        public OneOf(int choiceIndex, String... items) {
            this(choiceIndex, Arrays.asList(items));
        }

        public OneOf(int choiceIndex, List<String> items) {
            this.choiceIndex = choiceIndex;
            for (String item : items) {
                add(item);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + choiceIndex;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            OneOf other = (OneOf) obj;
            if (choiceIndex != other.choiceIndex)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return (choiceIndex == Phrases.COMMON_RULE ? "Common" : "choice " + choiceIndex) + " = " + super.toString();
        }

    }

    static class Rule extends ArrayList<OneOf> {
        private static final long serialVersionUID = 1L;

        final int group;
        final int index;

        public Rule(int group, int index) {
            this.group = group;
            this.index = index;
        }

        public Rule(int group, int index, String... items) {
            this.group = group;
            this.index = index;
            int choiceIndex = 0;
            for (String item : items) {
                add(new OneOf(choiceIndex++, item));
            }
        }

        public Rule(int group, int index, OneOf... items) {
            this(group, index, Arrays.asList(items));
        }

        public Rule(int group, int index, List<OneOf> items) {
            this.group = group;
            this.index = index;
            for (OneOf item : items) {
                add(item);
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + index;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            Rule other = (Rule) obj;
            if (index != other.index)
                return false;
            return true;
        }

        int choices() {
            return stream().reduce((a, b) -> {
                return a.choiceIndex > b.choiceIndex ? a : b;
            }).orElseGet(() -> new OneOf(0)).choiceIndex + 1;
        }

        public boolean containOptionalChoices() {
            for (OneOf items : this) {
                if (items.choiceIndex != Phrases.COMMON_RULE) {
                    if (items.size() == 1) {
                        if (items.iterator().next().isBlank()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "rule group=" + group + " index=" + index + " = " + super.toString();
        }

    }

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> strings) {
        return of(new Choices(strings.stream().map(Choice::new).collect(Collectors.toList())));
    }

    private Phrases() {
    }

    public Sequences<String> flatten() {
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

    public static Phrases of(Choices choices) {
        List<String> allPhrases = choices.stream().flatMap(choice -> choice.phrases.stream())
                .collect(Collectors.toList());
        Partition<String> phraseGroups = new Partition<>(allPhrases, Phrases::haveCommonParts);
        if (phraseGroups.groups.size() <= 1) {
            return sliceAllChoicesTogether(choices, allPhrases);
        } else {
            // TODO Group phrases and slice groups to improve common word rules
            return sliceEachChoiceSeparately(choices);
        }
    }

    private static Phrases sliceAllChoicesTogether(Choices choices, List<String> allPhrases) {
        List<StringSequences> sliced = StringSequences.slice(allPhrases);
        Phrases phrases = new Phrases();
        for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
            Rule rule = rule(choices, sliced, ruleIndex);
            // if (rule.containOptionalChoices()) {
            // // TODO First rule
            // phrases.add(phrases.join(phrases.getPrevious(rule), rule));
            // } else {
            // phrases.add(rule);
            // }
            phrases.add(rule);
        }
        return phrases;
    }

    private Rule join(Rule previous, Rule rule) {
        // TODO Refactor this so it doesn't look like an ugly hack
        remove(previous);
        Rule joinedRule = new Rule(rule.group, rule.index);
        for (OneOf items : rule) {
            OneOf joinedItems = new OneOf(items.choiceIndex);
            for (String string : items) {
                joinedItems.add(previous.iterator().next().iterator().next() + " " + string);
            }
            joinedRule.add(joinedItems);
        }
        return joinedRule;
    }

    private Rule getPrevious(Rule rule) {
        Rule previous = get(size() - 1);
        if (previous.index != rule.index - 1 || previous.group != rule.group)
            throw new IllegalStateException("Previous rule not found:" + rule);
        return previous;
    }

    static boolean haveCommonParts(String a, String b) {
        // TODO Optional parts must be at the same position
        List<StringSequences> slice = StringSequences.slice(a, b);
        // TODO turn this into production code
        boolean haveCommonParts = slice.stream().filter((StringSequences seq) -> {
            return seq.size() == 1;
        }).count() > 0;
        long c = slice.stream().filter((StringSequences seq) -> {
            long count = seq.stream().filter((Sequence<String> s) -> {
                return s.isEmpty();
            }).count();
            return count == 0;
        }).count();
        boolean areDefined = c == slice.size();
        // TODO define criteria for matching phrases
        areDefined = true;
        return haveCommonParts && areDefined;
    }

    static final int FIRST_GROUP = 0;

    private static Rule rule(Choices choices, List<StringSequences> sliced, int ruleIndex) {
        Rule rule = new Rule(FIRST_GROUP, ruleIndex);
        Sequences<String> sequences = sliced.get(ruleIndex);
        if (sequences.size() == 1) {
            rule.add(new OneOf(COMMON_RULE, sequences.get(0).toString()));
        } else {
            int itemIndex = 0;
            int choiceIndex = 0;
            for (Choice choice : choices) {
                int size = choice.phrases.size();
                OneOf oneOf = new OneOf(choiceIndex++, size);
                for (int i = 0; i < size; i++) {
                    String item = sequences.get(itemIndex + i).toString();
                    if (!oneOf.contains(item)) {
                        oneOf.add(item);
                    }
                }
                rule.add(oneOf);
                itemIndex += size;
            }
        }
        return rule;
    }

    private static Phrases sliceEachChoiceSeparately(Choices choices) {
        Phrases phrases = new Phrases();
        for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
            Choice choice = choices.get(choiceIndex);
            Partition<String> choiceGroups = new Partition<>(choice.phrases, Phrases::haveCommonParts);
            for (Partition<String>.Group group : choiceGroups.groups) {
                List<String> items = group.items;
                List<StringSequences> sliced = StringSequences.slice(items);

                for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
                    Rule rule = new Rule(choiceGroups.groups.indexOf(group), ruleIndex);
                    Sequences<String> sequences = sliced.get(ruleIndex);
                    if (sequences.size() == 1) {
                        rule.add(new OneOf(choiceIndex, sequences.get(0).toString()));
                    } else {
                        OneOf oneOf = createOneOfs(sequences, items, choiceIndex);
                        rule.add(oneOf);
                    }
                    phrases.add(rule);
                }
            }
        }
        return phrases;
    }

    private static OneOf createOneOfs(Sequences<String> sequences, List<String> items, int choiceIndex) {
        int size = items.size();
        OneOf oneOf = new OneOf(choiceIndex, size);
        for (int i = 0; i < size; i++) {
            String item = sequences.get(i).toString();
            if (!oneOf.contains(item)) {
                oneOf.add(item);
            }
        }
        return oneOf;
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
