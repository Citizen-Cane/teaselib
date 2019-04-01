package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Phrases extends ArrayList<Sequences<String>> {
    private static final long serialVersionUID = 1L;

    public static Phrases of(String... choices) {
        return of(Arrays.asList(choices));
    }

    public static Phrases of(List<String> choices) {
        return new Phrases(SequenceUtil.slice(choices));
    }

    public Phrases(List<Sequences<String>> phrases) {
        super(phrases);
    }

    public Sequences<String> flatten() {
        return Sequences.flatten(this);
    }

    public static Phrases ofPhrases(List<List<String>> phrases) {
        return of(phrases.stream().map(list -> list.get(0)).collect(Collectors.toList()));
    }
}
