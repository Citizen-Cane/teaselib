package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class Phrases extends ArrayList<Phrases.Rule> {
    private static final long serialVersionUID = 1L;

    public static Rule rule(int ruleIndex, String... items) {
        return new Rule(ruleIndex, items);
    }

    public static Rule rule(int ruleIndex, OneOf... items) {
        return new Rule(ruleIndex, items);
    }

    public static OneOf oneOf(int choiceIndex, String item) {
        return new Phrases.OneOf(choiceIndex, item);
    }

    public static OneOf oneOf(int choiceIndex, String... items) {
        return new Phrases.OneOf(choiceIndex, items);
    }

    static class OneOf extends ArrayList<String> {
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
    }

    static class Rule extends ArrayList<OneOf> {
        private static final long serialVersionUID = 1L;

        final int index;

        public Rule(int ruleIndex) {
            this.index = ruleIndex;
        }

        public Rule(int ruleIndex, String... items) {
            this.index = ruleIndex;
            int choiceIndex = 0;
            for (String item : items) {
                add(new OneOf(choiceIndex++, item));
            }
        }

        public Rule(int ruleIndex, OneOf... items) {
            this(ruleIndex, Arrays.asList(items));
        }

        public Rule(int ruleIndex, List<OneOf> items) {
            this.index = ruleIndex;
            for (OneOf item : items) {
                add(item);
            }
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
        int capaticy = maxLength();
        StringSequences flattened = StringSequences.ignoreCase(capaticy);
        for (int i = 0; i < capaticy; i++) {
            StringSequence sequence = StringSequence.ignoreCase();
            for (Rule elements : this) {
                if (elements.size() == 1) {
                    sequence.addAll(elements.get(0));
                } else {
                    sequence.addAll(elements.get(i));
                }
            }
            flattened.add(sequence);
        }
        return flattened;
    }

    public static Phrases of(Choices choices) {
        List<String> allPhrases = choices.stream().flatMap(choice -> choice.phrases.stream())
                .collect(Collectors.toList());
        List<StringSequences> sliced = StringSequences.slice(allPhrases);

        Phrases phrases = new Phrases();
        for (int index = 0; index < sliced.size(); index++) {
            phrases.add(rule(choices, sliced, index));
        }

        return phrases;
    }

    private static Rule rule(Choices choices, List<StringSequences> sliced, int ruleIndex) {
        Rule rule = new Rule(ruleIndex);
        Sequences<String> sequences = sliced.get(ruleIndex);
        if (sequences.size() == 1) {
            rule.add(new OneOf(0, sequences.get(0).toString()));
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

    public int maxLength() {
        Optional<Rule> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
