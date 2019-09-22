package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class StringSequences extends Sequences<String> {
    private static final long serialVersionUID = 1L;

    StringSequences(Collection<? extends Sequence<String>> elements, BiPredicate<String, String> equals) {
        super(elements, equals, StringSequences::joinCommon, StringSequences::joinSequence);
    }

    private StringSequences(int initialCapacity, BiPredicate<String, String> equals) {
        super(initialCapacity, equals, StringSequences::joinCommon, StringSequences::joinSequence);
    }

    protected static String joinCommon(List<String> elements) {
        return elements.get(0);
    }

    protected static String joinSequence(List<String> elements) {
        return String.join(" ", elements);
    }

    public static Sequences<String> of(int capacity) {
        return new StringSequences(capacity, String::equalsIgnoreCase);
    }

    public static List<Sequences<String>> of(String... choices) {
        return of(Arrays.asList(choices));
    }

    private static List<Sequences<String>> of(List<String> choices) {
        BiPredicate<String, String> equalsOp = String::equalsIgnoreCase;
        Function<String, List<String>> splitter = StringSequence::splitWords;
        return Sequences.of(choices, equalsOp, splitter, StringSequences::joinCommon, StringSequences::joinSequence);
    }

}
