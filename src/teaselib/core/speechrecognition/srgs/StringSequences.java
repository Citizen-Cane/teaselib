package teaselib.core.speechrecognition.srgs;

import static teaselib.core.speechrecognition.srgs.StringSequence.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class StringSequences extends Sequences<String> {
    private static final long serialVersionUID = 1L;

    StringSequences(Collection<? extends Sequence<String>> elements) {
        super(elements, Traits);
    }

    private StringSequences(int initialCapacity) {
        super(initialCapacity, Traits);
    }

    protected static String joinCommon(List<String> elements) {
        return elements.get(0);
    }

    protected static String joinSequence(List<String> elements) {
        return String.join(" ", elements);
    }

    public static Sequences<String> of(int capacity) {
        return new StringSequences(capacity);
    }

    public static List<Sequences<String>> of(String... choices) {
        return of(Arrays.asList(choices));
    }

    private static List<Sequences<String>> of(List<String> choices) {
        return Sequences.of(choices, Traits);
    }

}
