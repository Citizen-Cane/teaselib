package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
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

    public static class OneOf extends ArrayList<String> {
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

        public boolean hasOptionalParts() {
            return stream().anyMatch(String::isBlank);
        }

    }

    public static class Rule extends ArrayList<OneOf> {
        private static final long serialVersionUID = 1L;

        public final int group;
        public final int index;

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
            Optional<OneOf> reduce = stream().reduce((a, b) -> a.choiceIndex > b.choiceIndex ? a : b);
            if (reduce.isPresent()) {
                int choiceIndex = reduce.get().choiceIndex;
                return choiceIndex == Phrases.COMMON_RULE ? 1 : choiceIndex + 1;
            } else {
                return 1;
            }
        }

        public boolean containOptionalChoices() {
            return stream().anyMatch(
                    items -> items.choiceIndex != Phrases.COMMON_RULE && items.stream().anyMatch(String::isBlank));
        }

        public boolean isCommon() {
            return stream().reduce((a, b) -> {
                return a.choiceIndex > b.choiceIndex ? a : b;
            }).orElseGet(() -> new OneOf(COMMON_RULE)).choiceIndex == COMMON_RULE;
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
            phrases.add(rule);
        }

        phrases.joinRulesWithOptionalParts();
        return withEmptyRulesRemoved(phrases);
    }

    private static Phrases withEmptyRulesRemoved(Phrases phrases) {
        Phrases filtered = new Phrases();
        for (Rule rule : phrases) {
            if (!rule.isEmpty()) {
                filtered.add(rule);
            }
        }
        return filtered;
    }

    private void joinRulesWithOptionalParts() {
        int rules = rules();
        if (rules > 1) {
            for (Rule rule : this) {
                if (rule.index > 0 && rule.index < rules - 1) {
                    Rule previous = previous(rule);
                    Rule next = next(rule);
                    if (previous.containOptionalChoices() || next.containOptionalChoices()) {
                        Aside joined = joinAside(previous, rule, next);
                        set(indexOf(previous), joined.previous);
                        set(indexOf(next), joined.next);
                        set(indexOf(rule), placeholderOf(rule));
                    }
                } else if (rule.index == 0) {
                    Rule next = next(rule);
                    if (next.containOptionalChoices()) {
                        set(indexOf(next), joinCommonWithNext(rule, next));
                        set(indexOf(rule), placeholderOf(rule));
                    }
                } else if (rule.index == rules - 1) {
                    Rule previous = previous(rule);
                    if (previous.containOptionalChoices()) {
                        set(indexOf(previous), joinPreviousWithCommon(previous, rule));
                        set(indexOf(rule), placeholderOf(rule));
                    }
                }
            }
        }
    }

    private Rule previous(Rule rule) {
        return get(rule.group, rule.index - 1);
    }

    private Rule next(Rule rule) {
        return get(rule.group, rule.index + 1);
    }

    private static Rule placeholderOf(Rule previous) {
        return new Rule(previous.group, previous.index);
    }

    private Rule get(int group, int index) {
        for (Rule rule : this) {
            if (rule.index == index && rule.group == group) {
                return rule;
            }
        }
        throw new NoSuchElementException("group = " + group + ", index = " + index);
    }

    // TODO when a rule contains optional parts then split choices with optinal parts into separate group rules
    // TODO handle optional parts on both side of common words
    // TODO Move single words from one rule to the next
    static final class Aside {
        final Rule previous;
        final Rule next;

        Aside(Rule rule) {
            super();
            this.previous = new Rule(rule.group, rule.index - 1);
            this.next = new Rule(rule.group, rule.index + 1);
        }
    }

    private static Aside joinAside(Rule previous, Rule common, Rule next) {
        Aside rules = new Aside(common);
        int choices = next.size();
        if (previous.size() != choices) {
            throw new IllegalArgumentException("Rule choice mismatch: " + previous + " <->" + next);
        }

        String commonWords = getCommonWordsFrom(common);

        for (int choiceIndex = 0; choiceIndex < choices; choiceIndex++) {
            OneOf previousItems = previous.get(choiceIndex);
            OneOf nextItems = next.get(choiceIndex);

            // TODO Reduce code duplication to first condition
            if (previousItems.hasOptionalParts() && nextItems.hasOptionalParts()) {
                joinPreviousAndNext(rules, commonWords, previousItems, nextItems);
            } else if (previousItems.hasOptionalParts()) {
                joinPreviousWIthCommon(rules, commonWords, previousItems, nextItems);
            } else if (nextItems.hasOptionalParts()) {
                joinCommonWithNext(rules, commonWords, previousItems, nextItems);
            } else {
                joinUnnecessaryButCannotAvoidRightNow(rules, commonWords, previousItems, nextItems);
            }
        }
        return rules;
    }

    private static void joinPreviousAndNext(Aside rules, String commonWords, OneOf previousItems, OneOf nextItems) {
        OneOf joinedPreviousItems = new OneOf(previousItems.choiceIndex);
        OneOf joinedNextItems = new OneOf(nextItems.choiceIndex);
        for (int i = 0; i < previousItems.size(); i++) {
            String stringPrevious = previousItems.get(i);
            String stringNext = nextItems.get(i);

            if (stringPrevious.isBlank() && stringNext.isBlank()) {
                throw new UnsupportedOperationException("TODO reduce optional words on both sides to new group");
            } else if (stringPrevious.isBlank()) {
                joinedPreviousItems.add(commonWords);
                joinedNextItems.add(stringNext);
            } else if (stringNext.isBlank()) {
                joinedPreviousItems.add(stringPrevious);
                joinedNextItems.add(commonWords);
            } else {
                joinedPreviousItems.add(stringPrevious + " " + commonWords);
                joinedNextItems.add(commonWords + " " + stringNext);
            }
        }
        rules.previous.add(joinedPreviousItems);
        rules.next.add(joinedNextItems);
    }

    private static void joinPreviousWIthCommon(Aside rules, String commonWords, OneOf previousItems, OneOf nextItems) {
        OneOf joinedPreviousItems = new OneOf(previousItems.choiceIndex);
        OneOf joinedNextItems = new OneOf(nextItems.choiceIndex);
        for (int i = 0; i < previousItems.size(); i++) {
            String stringPrevious = previousItems.get(i);
            String stringNext = nextItems.get(i);
            if (!stringPrevious.isBlank()) {
                joinedPreviousItems.add(stringPrevious + " " + commonWords);
            } else {
                joinedPreviousItems.add(commonWords);
            }
            // TODO replace with original, revert to enhanced for-loop
            joinedNextItems.add(stringNext);
        }
        rules.previous.add(joinedPreviousItems);
        rules.next.add(nextItems);
    }

    private static void joinCommonWithNext(Aside rules, String commonWords, OneOf previousItems, OneOf nextItems) {
        OneOf joinedPreviousItems = new OneOf(previousItems.choiceIndex);
        OneOf joinedNextItems = new OneOf(nextItems.choiceIndex);
        for (int i = 0; i < nextItems.size(); i++) {
            String stringPrevious = previousItems.get(i);
            String stringNext = nextItems.get(i);
            if (!stringNext.isBlank()) {
                joinedNextItems.add(commonWords + " " + stringNext);
            } else {
                joinedNextItems.add(commonWords);
            }
            // TODO replace with original, revert to enhanced for-loop
            joinedPreviousItems.add(stringPrevious);
        }
        rules.previous.add(previousItems);
        rules.next.add(joinedNextItems);
    }

    private static void joinUnnecessaryButCannotAvoidRightNow(Aside rules, String commonWords, OneOf previousItems,
            OneOf nextItems) {
        OneOf joinedPreviousItems = new OneOf(previousItems.choiceIndex);
        OneOf joinedNextItems = new OneOf(nextItems.choiceIndex);
        // throw new IllegalArgumentException("Joining is only allowed for optional choices");
        // Unaffected choices (without optional items) must be joined as well
        // TODO -> split rules so that each choice is in a separate group
        // - rules may not contain multiple choices anymore
        // - can keep rules together if all or no choices contain optional items
        // TODO remove duplicated code from if-clause "previousItem"
        for (int i = 0; i < previousItems.size(); i++) {
            String stringPrevious = previousItems.get(i);
            String stringNext = nextItems.get(i);
            joinedPreviousItems.add(stringPrevious + " " + commonWords);
            // TODO replace with original, revert to enhanced for-loop
            joinedNextItems.add(stringNext);
        }
        rules.previous.add(joinedPreviousItems);
        rules.next.add(nextItems);
    }

    // TODO Move single words from one rule to the next
    private static Rule joinCommonWithNext(Rule common, Rule next) {
        // TODO Refactor this so it doesn't look like an ugly hack
        String commonWords = getCommonWordsFrom(common);

        Rule joinedRule = new Rule(next.group, next.index);
        for (OneOf items : next) {
            OneOf joinedItems = new OneOf(items.choiceIndex);
            for (String string : items) {
                if (!string.isBlank()) {
                    joinedItems.add(commonWords + " " + string);
                } else {
                    joinedItems.add(commonWords);
                }
            }
            // TODO Re-index follow-up rules, or insert dummy rule that is ignored by SRGSBuilder
            joinedRule.add(joinedItems);
        }
        return joinedRule;
    }

    private static Rule joinPreviousWithCommon(Rule previous, Rule common) {
        // TODO Refactor this so it doesn't look like an ugly hack
        String commonWords = getCommonWordsFrom(common);

        Rule joinedRule = new Rule(previous.group, previous.index);
        for (OneOf items : previous) {
            OneOf joinedItems = new OneOf(items.choiceIndex);
            for (String string : items) {
                if (!string.isBlank()) {
                    joinedItems.add(string + " " + commonWords);
                } else {
                    joinedItems.add(commonWords);
                }
            }
            // TODO Re-index follow-up rules, or insert dummy rule that is ignored by SRGSBuilder
            joinedRule.add(joinedItems);
        }
        return joinedRule;
    }

    private static String getCommonWordsFrom(Rule common) {
        OneOf item = common.iterator().next();
        if (item.choiceIndex != Phrases.COMMON_RULE) {
            throw new IllegalArgumentException(common.toString());
        }

        return item.iterator().next();
    }

    static boolean haveCommonParts(String a, String b) {
        List<StringSequences> slice = StringSequences.slice(a, b);
        boolean sequencesShareCommonParts = slice.stream().anyMatch(sequence -> sequence.size() == 1);
        boolean withoutOptionalPartMismatches = slice.stream()
                .noneMatch((StringSequences seq) -> seq.stream().anyMatch(Sequence<String>::isEmpty));
        return sequencesShareCommonParts && withoutOptionalPartMismatches;
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
                    // Optimize after optimizing optional rules in SRGSBuilder
                    // TODO Resolve code duplication with createOneOf
                    // if (!oneOf.contains(item)) {
                    oneOf.add(item);
                    // }
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
            // Optimize after optimizing optional rules in SRGSBuilder
            // if (!oneOf.contains(item)) {
            oneOf.add(item);
            // }
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
