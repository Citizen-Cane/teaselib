package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListSequenceUtil {

    private ListSequenceUtil() {
    }

    public static String[] splitWords(String string) {
        if (string.isEmpty()) {
            return new String[] {};
        } else {
            return string.split("[ .:,;\t\n_()]+");
        }
    }

    public static List<ListSequences<String>> slice(String... choices) {
        return slice(Arrays.asList(choices));
    }

    public static List<ListSequences<String>> slice(List<String> choices) {
        ListSequences<String> sequences = new ListSequences<>(choices.size());
        for (String choice : choices) {
            sequences.add(new ListSequence<>(splitWords(choice)));
        }
        return sliceSequences(sequences);
    }

    public static List<ListSequences<String>> sliceSequences(ListSequences<String> choices) {
        List<ListSequences<String>> slices = new ArrayList<>();

        ListSequence<String> commonStart = choices.commonStart();
        if (!commonStart.isEmpty()) {
            slices.add(new ListSequences<String>(commonStart));
        }
        ListSequences<String> remainder = commonStart.isEmpty() ? choices : choices.removeIncluding(commonStart);

        while (!remainder.get(0).isEmpty()) {
            ListSequence<String> commonMiddle = remainder.commonMiddle();
            if (!commonMiddle.isEmpty()) {
                ListSequences<String> gap = remainder.removeFrom(commonMiddle);
                slices.add(gap);
                slices.add(new ListSequences<String>(commonMiddle));
            }

            if (commonMiddle.isEmpty()) {
                slices.add(remainder);
                break;
            }
            remainder = remainder.removeIncluding(commonMiddle);
        }
        return slices;
    }

}
