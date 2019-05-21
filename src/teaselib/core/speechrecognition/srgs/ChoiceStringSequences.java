package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class ChoiceStringSequences extends Sequences<ChoiceString> {
    private static final long serialVersionUID = 1L;

    private ChoiceStringSequences() {
        super(ChoiceString::samePhrase, ChoiceString::joinCommon, ChoiceString::joinSequence);
    }

    public static List<Sequences<ChoiceString>> slice(ChoiceString... choices) {
        return slice(Arrays.asList(choices));
    }

    public static List<Sequences<ChoiceString>> slice(List<ChoiceString> choices) {
        BiPredicate<ChoiceString, ChoiceString> equalsOp = ChoiceString::samePhrase;
        Function<ChoiceString, List<ChoiceString>> splitter = ChoiceString::words;
        return Sequences.of(choices, equalsOp, splitter, ChoiceString::joinCommon, ChoiceString::joinSequence,
                s -> new ChoiceString("", s.choice));
    }

    public static <T> int max(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
