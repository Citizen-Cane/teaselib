package teaselib.core.speechrecognition.srgs;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public class ChoiceStringSequence extends Sequence<ChoiceString> {
    private static final long serialVersionUID = 1L;

    private ChoiceStringSequence(List<ChoiceString> elements, BiPredicate<ChoiceString, ChoiceString> equals) {
        super(elements, equals);
    }

    private ChoiceStringSequence(ChoiceString[] elements, BiPredicate<ChoiceString, ChoiceString> equals) {
        super(elements, equals);
    }

    public ChoiceStringSequence(Sequence<ChoiceString> sequence) {
        super(sequence, sequence.equalsOperator);
    }

    public static ChoiceStringSequence ignoreCase() {
        return new ChoiceStringSequence(Collections.emptyList(), ChoiceString::equalsIgnoreCase);
    }

    public static ChoiceStringSequence ignoreCase(ChoiceString string) {
        return new ChoiceStringSequence(string.words(), ChoiceString::equalsIgnoreCase);
    }

    public static ChoiceStringSequence ignoreCase(List<ChoiceString> elements) {
        return new ChoiceStringSequence(elements, ChoiceString::equalsIgnoreCase);
    }

    public static ChoiceStringSequence of(Sequence<ChoiceString> sequence) {
        return new ChoiceStringSequence(sequence);
    }
}
