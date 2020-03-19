package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toList;
import static teaselib.core.speechrecognition.srgs.PhraseString.Traits;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class PhraseStringSequences extends Sequences<PhraseString> {
    private static final long serialVersionUID = 1L;

    @SafeVarargs
    PhraseStringSequences(Sequence<PhraseString>... choices) {
        this(Stream.of(choices).collect(toList()));
    }

    PhraseStringSequences(Collection<Sequence<PhraseString>> choices) {
        super(choices, Traits);
    }

    public static Sequences<PhraseString> of(List<PhraseString> choices) {
        return Sequences.of(choices, Traits);
    }

}
