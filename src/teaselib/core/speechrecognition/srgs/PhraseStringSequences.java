package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.stream.Stream;

public class PhraseStringSequences extends Sequences<PhraseString> {
    private static final long serialVersionUID = 1L;

    @SafeVarargs
    PhraseStringSequences(Sequence<PhraseString>... choices) {
        this(Stream.of(choices).collect(toList()));
    }

    PhraseStringSequences(List<Sequence<PhraseString>> choices) {
        super(choices, PhraseString::samePhrase, PhraseString::joinCommon, PhraseString::joinSequence);
    }

    public static List<Sequences<PhraseString>> slice(List<PhraseString> choices) {
        return Sequences.of(choices, PhraseString::samePhrase, PhraseString::words, PhraseString::joinCommon,
                PhraseString::joinSequence);
    }

}
