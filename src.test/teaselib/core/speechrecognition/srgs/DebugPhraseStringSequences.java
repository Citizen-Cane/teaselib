package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Collectors;

public class DebugPhraseStringSequences extends PhraseStringSequences {
    private static final long serialVersionUID = 1L;

    public DebugPhraseStringSequences(List<Sequence<PhraseString>> choices) {
        super(choices);
    }

    @Override
    public String toString() {
        StringBuilder phrases = new StringBuilder();
        phrases.append(super.toString());
        phrases.append("\n");

        phrases.append(this
                .stream().map(phrase -> phrase.stream()
                        .map(element -> "\t\"" + element.phrase + "\"=" + element.indices + " ").collect(joining(" ")))
                .collect(Collectors.joining("\n")));

        return phrases.toString();
    }

}
