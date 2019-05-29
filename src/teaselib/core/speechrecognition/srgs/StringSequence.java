package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public class StringSequence extends Sequence<String> {
    private static final long serialVersionUID = 1L;

    private StringSequence(List<String> elements, BiPredicate<String, String> equals) {
        super(elements, equals);
    }

    public static List<String> splitWords(String string) {
        if (string.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(string.split("[ .:,;\t\n_()]+"));
        }
    }

    public static StringSequence ignoreCase() {
        return new StringSequence(Collections.emptyList(), String::equalsIgnoreCase);
    }

    public static StringSequence ignoreCase(String string) {
        return new StringSequence(splitWords(string), String::equalsIgnoreCase);
    }

}
