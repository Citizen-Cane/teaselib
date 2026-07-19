package teaselib.core.speechrecognition.srgs;

import static teaselib.core.speechrecognition.srgs.StringSequence.Traits;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

public class StringSequences extends Sequences<String> {

    StringSequences(int capacity) {
        super(capacity, Traits);
    }

    StringSequences(Collection<? extends Sequence<String>> elements) {
        super(elements, Traits);
    }

    protected static String joinCommon(List<String> elements) {
        return elements.getFirst();
    }

    protected static String joinSequence(List<String> elements) {
        return String.join(" ", elements);
    }

    protected static final BiPredicate<List<String>, List<String>> joinableSequences = (a, b) -> true;

}
