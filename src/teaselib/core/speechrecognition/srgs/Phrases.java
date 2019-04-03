package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;

public class Phrases extends ArrayList<List<Sequence<String>>> {
    private static final long serialVersionUID = 1L;

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> strings) {
        return new Phrases(SequenceUtil.slice(strings));
    }

    private Phrases(List<Sequences<String>> phrases) {
        super(phrases);
    }

    public Sequences<String> flatten() {
        return Sequences.flatten(this);
    }

    public static Phrases ofPhrases(List<List<String>> phrases) {
        return of(phrases.stream().map(list -> {
            if (list.size() > 1) {
                throw new UnsupportedOperationException("Multiple phrases: " + list);
            }
            // TODO Build phrases object with multiple phrase alternatives
            return list.get(0);
        }).collect(Collectors.toList()));
    }

    public static Phrases of(Choices choices) {
        List<String> allPhrases = choices.stream().flatMap(choice -> choice.phrases.stream())
                .collect(Collectors.toList());
        List<Sequences<String>> sliced = SequenceUtil.slice(allPhrases);

        int ruleIndex = 0;
        AlternativePhrases phrases = new AlternativePhrases();
        for (Sequences<String> slice : sliced) {
            Rule rule = new Rule();
            phrases.add(rule);
            int itemIndex = 0;
            Sequences<String> sequences = sliced.get(ruleIndex);
            if (sequences.size() == 1) {
                OneOf oneOf = new OneOf();
                rule.add(oneOf);
                oneOf.add(sequences.get(itemIndex).toString());
            } else {
                for (Choice choice : choices) {
                    OneOf oneOf = new OneOf();
                    rule.add(oneOf);
                    for (int i = 0; i < choice.phrases.size(); i++) {
                        oneOf.add(sequences.get(itemIndex + i).toString());
                    }
                    itemIndex += choice.phrases.size();
                }
            }
            ruleIndex++;
        }
        // TODO Phrases of all alternatives
        return null;
        // of(phrases.stream().map(list -> list.get(0)).collect(Collectors.toList()));
    }

    // static class Item extends Sequence<String> {
    // }

    static class OneOf extends ArrayList<String> {
    }

    static class Rule extends ArrayList<OneOf> {
    }

    public static class AlternativePhrases extends ArrayList<Rule> {

        public static AlternativePhrases of(Choices choices) {
            return null;
        }
    }
}
