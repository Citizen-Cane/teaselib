package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.util.List;

public class StringSequence extends Sequence<String> {
    private static final long serialVersionUID = 1L;

    public static final Traits<String> Traits = new Traits<>(String::equalsIgnoreCase, StringSequence::splitWords,
            StringSequence::commonness, StringSequences::joinCommon, StringSequences::joinSequence);

    private StringSequence(List<String> elements) {
        super(elements, Traits);
    }

    public static List<String> splitWords(String string) {
        if (string.isEmpty()) {
            return emptyList();
        } else {
            return asList(string.split("[ .:,;\t\n_()]+"));
        }
    }

    private static int commonness(List<Sequence<String>> elements) {
        return elements.stream().map(element -> -element.size()).reduce(Math::max).orElse(Integer.MIN_VALUE);
    }

    public static StringSequence ignoreCase() {
        return new StringSequence(emptyList());
    }

    public static StringSequence ignoreCase(String string) {
        return new StringSequence(splitWords(string));
    }

}
