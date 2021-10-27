package teaselib.core.speechrecognition.srgs;

import static teaselib.core.speechrecognition.srgs.StringSequence.*;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

public class StringSequences extends Sequences<String> {
    private static final long serialVersionUID = 1L;

    StringSequences(Collection<? extends Sequence<String>> elements) {
        super(elements, Traits);
    }

    protected static String joinCommon(List<String> elements) {
        return elements.get(0);
    }

    protected static String joinSequence(List<String> elements) {
        return String.join(" ", elements);
    }

    protected static final BiPredicate<List<String>, List<String>> joinableSequences = (a, b) -> true;

}
