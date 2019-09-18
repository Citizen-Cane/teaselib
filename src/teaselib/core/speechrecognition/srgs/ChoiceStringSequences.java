package teaselib.core.speechrecognition.srgs;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChoiceStringSequences extends Sequences<ChoiceString> {
    private static final long serialVersionUID = 1L;

    private ChoiceStringSequences() {
        super(ChoiceString::samePhrase, ChoiceString::joinCommon, ChoiceString::joinSequence);
    }

    ChoiceStringSequences(ChoiceString... strings) {
        super(Stream.of(strings).map(Sequence::new).collect(Collectors.toList()), ChoiceString::samePhrase,
                ChoiceString::joinCommon, ChoiceString::joinSequence);
    }

    public static List<Sequences<ChoiceString>> slice(List<ChoiceString> choices) {
        BiPredicate<ChoiceString, ChoiceString> equalsOp = ChoiceString::samePhrase;
        Function<ChoiceString, List<ChoiceString>> splitter = ChoiceString::words;
        return Sequences.of(choices, equalsOp, splitter, ChoiceString::joinCommon, ChoiceString::joinSequence,
                s -> new ChoiceString("", s.choices));
    }

}
