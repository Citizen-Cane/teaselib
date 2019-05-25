package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.util.math.Partition;

/**
 * @author Citizen-Cane
 *
 */
public class SimplifiedPhrases {

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> choices) {
        return of(new Choices(choices.stream().map(Choice::new).collect(Collectors.toList())));
    }

    public static Phrases of(Choices choices) {
        List<String> allPhrases = choices.stream().flatMap(choice -> choice.phrases.stream())
                .collect(Collectors.toList());
        Partition<String> phraseGroups = new Partition<>(allPhrases, SimplifiedPhrases::haveCommonParts);
        if (phraseGroups.groups.size() <= 1) {
            return sliceAllChoicesTogether(choices, allPhrases);
        } else {
            // TODO Group phrases and slice groups to improve common word rules
            return sliceEachChoiceSeparately(choices);
        }
    }

    private static Phrases sliceAllChoicesTogether(Choices choices, List<String> allPhrases) {
        List<Sequences<String>> sliced = StringSequences.of(allPhrases);
        Phrases phrases = new Phrases(choices.size());
        for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
            Rule rule = rule(choices, sliced, ruleIndex);
            phrases.add(rule);
        }

        joinRulesWithOptionalParts(phrases);
        return withEmptyRulesRemoved(phrases);
    }

    private static Phrases withEmptyRulesRemoved(Phrases phrases) {
        Phrases filtered = new Phrases(phrases.choiceCount);
        for (Rule rule : phrases) {
            if (!rule.isEmpty()) {
                filtered.add(rule);
            }
        }
        return filtered;
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

    static void joinRulesWithOptionalParts(Phrases phrases) {
        int rules = phrases.rules();
        if (rules > 1) {
            for (Rule rule : phrases) {
                if (rule.index > 0 && rule.index < rules - 1) {
                    Rule previous = phrases.previous(rule);
                    Rule next = phrases.next(rule);
                    if (previous.containOptionalChoices() || next.containOptionalChoices()) {
                        Aside joined = joinAside(previous, rule, next);
                        phrases.set(phrases.indexOf(previous), joined.previous);
                        phrases.set(phrases.indexOf(next), joined.next);
                        phrases.set(phrases.indexOf(rule), Rule.placeholderOf(rule));
                    }
                } else if (rule.index == 0) {
                    Rule next = phrases.next(rule);
                    if (next.containOptionalChoices()) {
                        phrases.set(phrases.indexOf(next), joinCommonWithNext(rule, next));
                        phrases.set(phrases.indexOf(rule), Rule.placeholderOf(rule));
                    }
                } else if (rule.index == rules - 1) {
                    Rule previous = phrases.previous(rule);
                    if (previous.containOptionalChoices()) {
                        phrases.set(phrases.indexOf(previous), joinPreviousWithCommon(previous, rule));
                        phrases.set(phrases.indexOf(rule), Rule.placeholderOf(rule));
                    }
                }
            }
        }
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
        List<Sequences<String>> slice = StringSequences.of(a, b);
        boolean sequencesShareCommonParts = slice.stream().anyMatch(sequence -> sequence.size() == 1);
        boolean withoutOptionalPartMismatches = slice.stream()
                .noneMatch(seq -> seq.stream().anyMatch(Sequence<String>::isEmpty));
        return sequencesShareCommonParts && withoutOptionalPartMismatches;
    }

    static final int FIRST_GROUP = 0;

    private static Rule rule(Choices choices, List<Sequences<String>> sliced, int ruleIndex) {
        Rule rule = new Rule(FIRST_GROUP, ruleIndex);
        Sequences<String> sequences = sliced.get(ruleIndex);
        if (sequences.size() == 1) {
            rule.add(new OneOf(Phrases.COMMON_RULE, sequences.get(0).toString()));
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
        Phrases phrases = new Phrases(choices.size());
        for (int choiceIndex = 0; choiceIndex < choices.size(); choiceIndex++) {
            Choice choice = choices.get(choiceIndex);
            Partition<String> choiceGroups = new Partition<>(choice.phrases, SimplifiedPhrases::haveCommonParts);
            for (Partition<String>.Group group : choiceGroups.groups) {
                List<String> items = group.items;
                List<Sequences<String>> sliced = StringSequences.of(items);

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

}
