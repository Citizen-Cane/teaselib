package teaselib.core.speechrecognition.srgs;

import java.util.Collection;
import java.util.stream.Collectors;

public class DebugPhraseStringSequencesList extends SlicedPhrases<PhraseString> {

    public DebugPhraseStringSequencesList(Collection<? extends Sequences<PhraseString>> c) {
        super(c.stream().map(DebugPhraseStringSequences::new).collect(Collectors.toList()));
    }

    public DebugPhraseStringSequencesList() {
        super();
    }

    @Override
    public String toString() {
        StringBuilder phrases = new StringBuilder();
        phrases.append("Commonness=");
        phrases.append(averageCommonness());
        phrases.append(" ");
        phrases.append("Symbol count=");
        phrases.append(symbolCount());
        phrases.append(" ");
        phrases.append("duplicated=");
        phrases.append(duplicatedSymbolsCount());
        phrases.append("\n");

        phrases.append(stream().map(Object::toString).collect(Collectors.joining("\n")));
        return phrases.toString();
    }

}
