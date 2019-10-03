package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

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

    public static int commonness(String element) {
        return 1;
    }

    public static StringSequence ignoreCase() {
        return new StringSequence(emptyList());
    }

    public static StringSequence ignoreCase(String string) {
        return new StringSequence(splitWords(string));
    }

}
