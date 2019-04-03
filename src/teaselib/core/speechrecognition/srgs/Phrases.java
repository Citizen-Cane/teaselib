package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        // TODO Phrases of all alternatives
        return null;
        // of(phrases.stream().map(list -> list.get(0)).collect(Collectors.toList()));
    }

}
