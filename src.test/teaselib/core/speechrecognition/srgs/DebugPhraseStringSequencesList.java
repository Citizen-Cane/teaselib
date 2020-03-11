package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class DebugPhraseStringSequencesList extends ArrayList<Sequences<PhraseString>> {
    private static final long serialVersionUID = 1L;

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
        phrases.append(Sequences.averageCommonness(this));
        phrases.append("\n");

        phrases.append(this.stream().map(Object::toString).collect(Collectors.joining("\n")));
        return phrases.toString();
    }

}
