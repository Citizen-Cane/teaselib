package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

public class ChoiceStringSequences extends Sequences<ChoiceString> {
    private static final long serialVersionUID = 1L;

    private ChoiceStringSequences() {
        super(ChoiceString::samePhrase, ChoiceString::joinCommon, ChoiceString::joinSequence);
    }

    @SafeVarargs
    ChoiceStringSequences(Sequence<ChoiceString>... choices) {
        super(Stream.of(choices).collect(toList()), ChoiceString::samePhrase, ChoiceString::joinCommon,
                ChoiceString::joinSequence);
    }

    public static List<Sequences<ChoiceString>> slice(List<ChoiceString> choices) {
        return Sequences.of(choices, ChoiceString::samePhrase, ChoiceString::words, ChoiceString::joinCommon,
                ChoiceString::joinSequence, s -> new ChoiceString("", s.choices));
    }

}
