package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class StringSequences extends Sequences<String> {
    private static final long serialVersionUID = 1L;

    public StringSequences(StringSequence sequence) {
        this(Collections.singletonList(sequence), sequence.equalsOperator);
    }

    private StringSequences(Collection<? extends Sequence<String>> elements, BiPredicate<String, String> equals) {
        super(elements, equals);
    }

    private StringSequences(int initialCapacity, BiPredicate<String, String> equals) {
        super(initialCapacity, equals);
    }

    private static StringSequences of(Sequences<String> sequences) {
        return new StringSequences(sequences, sequences.equalsOperator);
    }

    public static StringSequences ignoreCase(int capacity) {
        return new StringSequences(capacity, String::equalsIgnoreCase);
    }

    public static StringSequences ignoreCase(String... strings) {
        List<StringSequence> sequences = Arrays.stream(strings).map(StringSequence::ignoreCase)
                .collect(Collectors.toList());
        return new StringSequences(sequences, String::equalsIgnoreCase);
    }

    public static List<StringSequences> slice(String... choices) {
        return slice(Arrays.asList(choices));
    }

    public static List<StringSequences> slice(List<String> choices) {
        StringSequences sequences = StringSequences.ignoreCase(choices.size());
        for (String choice : choices) {
            sequences.add(StringSequence.ignoreCase(choice));
        }
        return slice(sequences);
    }

    public static List<StringSequences> slice(StringSequences choices) {
        List<StringSequences> slices = new ArrayList<>();

        StringSequence commonStart = StringSequence.of(choices.commonStart());
        if (!commonStart.isEmpty()) {
            slices.add(new StringSequences(commonStart));
        }
        Sequences<String> remainder = commonStart.isEmpty() ? choices : choices.removeIncluding(commonStart);

        while (remainder.maxLength() > 0) {
            StringSequence commonMiddle = StringSequence.of(remainder.commonMiddle());
            if (!commonMiddle.isEmpty()) {
                slices.add(new StringSequences(remainder.removeFrom(commonMiddle), choices.equalsOperator));
                slices.add(new StringSequences(commonMiddle));
            }

            if (commonMiddle.isEmpty()) {
                slices.add(StringSequences.of(remainder));
                break;
            }
            remainder = remainder.removeIncluding(commonMiddle);
        }
        return slices;
    }

    public static <T> int max(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
