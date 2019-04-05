package teaselib.core.speechrecognition.srgs;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public class StringSequence extends Sequence<String> {
    private static final long serialVersionUID = 1L;

    private StringSequence(List<String> elements, BiPredicate<String, String> equals) {
        super(elements, equals);
    }

    private StringSequence(String[] elements, BiPredicate<String, String> equals) {
        super(elements, equals);
    }

    public StringSequence(Sequence<String> commonMiddle) {
        super(commonMiddle, commonMiddle.equalsOperator);
    }

    public static String[] splitWords(String string) {
        if (string.isEmpty()) {
            return new String[] {};
        } else {
            return string.split("[ .:,;\t\n_()]+");
        }
    }

    public static StringSequence ignoreCase() {
        return new StringSequence(Collections.emptyList(), String::equalsIgnoreCase);
    }

    public static StringSequence ignoreCase(String string) {
        return new StringSequence(splitWords(string), String::equalsIgnoreCase);
    }

    public static StringSequence ignoreCase(List<String> elements) {
        return new StringSequence(elements, String::equalsIgnoreCase);
    }

    public static StringSequence of(Sequence<String> sequence) {
        return new StringSequence(sequence);
    }
}
