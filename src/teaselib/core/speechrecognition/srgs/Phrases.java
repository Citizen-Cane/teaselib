package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class Phrases extends ArrayList<Phrases.Rule> {
    private static final long serialVersionUID = 1L;

    public static Rule rule(String... items) {
        return new Rule(items);
    }

    public static Rule rule(OneOf... items) {
        return new Rule(items);
    }

    public static OneOf oneOf(String item) {
        return new Phrases.OneOf(item);
    }

    public static OneOf oneOf(String... items) {
        return new Phrases.OneOf(items);
    }

    static class OneOf extends ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public OneOf() {
        }

        public OneOf(int capacity) {
            super(capacity);
        }

        public OneOf(String item) {
            add(item);
        }

        public OneOf(String... items) {
            for (String item : items) {
                add(item);
            }
        }
    }

    static class Rule extends ArrayList<OneOf> {
        private static final long serialVersionUID = 1L;

        public Rule() {
        }

        public Rule(String... items) {
            for (String item : items) {
                add(new OneOf(item));
            }
        }

        public Rule(OneOf... items) {
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
        int length = maxLength();
        Sequences<String> flattened = new Sequences<>(length);
        for (int i = 0; i < length; i++) {
            Sequence<String> sequence = new Sequence<>();
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
        List<Sequences<String>> sliced = SequenceUtil.slice(allPhrases);

        Phrases phrases = new Phrases();
        for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
            Rule rule = new Rule();
            phrases.add(rule);
            int itemIndex = 0;
            Sequences<String> sequences = sliced.get(ruleIndex);
            if (sequences.size() == 1) {
                rule.add(new OneOf(sequences.get(itemIndex).toString()));
            } else {
                for (Choice choice : choices) {
                    int size = choice.phrases.size();
                    OneOf oneOf = new OneOf(size);
                    for (int i = 0; i < size; i++) {
                        // TODO Eliminate duplicates here or when building xml
                        oneOf.add(sequences.get(itemIndex + i).toString());
                    }
                    rule.add(oneOf);
                    itemIndex += size;
                }
            }
        }

        return phrases;
    }

    public int maxLength() {
        Optional<Rule> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
