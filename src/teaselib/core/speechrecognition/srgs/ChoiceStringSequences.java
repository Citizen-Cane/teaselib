package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChoiceStringSequences extends Sequences<ChoiceString> {
    private static final long serialVersionUID = 1L;

    public ChoiceStringSequences(ChoiceStringSequence sequence) {
        this(Collections.singletonList(sequence), sequence.equalsOperator);
    }

    private ChoiceStringSequences(Collection<? extends Sequence<ChoiceString>> elements,
            BiPredicate<ChoiceString, ChoiceString> equals) {
        super(elements, equals);
    }

    private ChoiceStringSequences(int initialCapacity, BiPredicate<ChoiceString, ChoiceString> equals) {
        super(initialCapacity, equals);
    }

    // private static ChoiceStringSequences of(Sequences<ChoiceString> sequences) {
    // return new ChoiceStringSequences(sequences, sequences.equalsOperator);
    // }

    public static ChoiceStringSequences ignoreCase(int capacity) {
        return new ChoiceStringSequences(capacity, ChoiceString::equalsIgnoreCase);
    }

    public static ChoiceStringSequences ignoreCase(ChoiceString... strings) {
        List<ChoiceStringSequence> sequences = Arrays.stream(strings).map(ChoiceStringSequence::ignoreCase)
                .collect(Collectors.toList());
        return new ChoiceStringSequences(sequences, ChoiceString::equalsIgnoreCase);
    }

    public static List<Sequences<ChoiceString>> slice(ChoiceString... choices) {
        return slice(Arrays.asList(choices));
    }

    public static List<Sequences<ChoiceString>> slice(List<ChoiceString> choices) {
        BiPredicate<ChoiceString, ChoiceString> equalsOp = ChoiceString::samePhrase;
        Function<ChoiceString, List<ChoiceString>> splitter = ChoiceString::words;
        return Sequences.of(choices, equalsOp, splitter);
        // if (choices.isEmpty()) {
        // return Collections.emptyList();
        // } else {
        // ChoiceStringSequences sequences = ChoiceStringSequences.ignoreCase(choices.size());
        // for (ChoiceString choice : choices) {
        // sequences.add(ChoiceStringSequence.ignoreCase(choice));
        // }
        // return slice(sequences);
        // }
    }

    // TODO Use templated version
    // @Deprecated
    // public static List<Sequences<ChoiceString>> slice(Sequences<ChoiceString> choices) {
    // List<Sequences<ChoiceString>> slices = new ArrayList<>();
    //
    // Sequence<ChoiceString> commonStart = choices.commonStart();
    // if (!commonStart.isEmpty()) {
    // slices.add(new Sequences<ChoiceString>(commonStart));
    // }
    // Sequences<ChoiceString> remainder = commonStart.isEmpty() ? choices : choices.removeIncluding(commonStart);
    //
    // while (remainder.maxLength() > 0) {
    // Sequence<ChoiceString> commonMiddle = remainder.commonMiddle();
    // if (!commonMiddle.isEmpty()) {
    // Sequences<ChoiceString> unique = remainder.removeUpTo(commonMiddle);
    // slices.add(new ChoiceStringSequences(unique, choices.equalsOperator));
    // slices.add(new Sequences<ChoiceString>(commonMiddle));
    // }
    //
    // if (commonMiddle.isEmpty()) {
    // slices.add(remainder);
    // break;
    // }
    // remainder = remainder.removeIncluding(commonMiddle);
    // }
    // return slices;
    // }

    public static <T> int max(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
