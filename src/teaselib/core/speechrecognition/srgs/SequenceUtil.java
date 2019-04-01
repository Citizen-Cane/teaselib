package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SequenceUtil {

    private SequenceUtil() {
    }

    public static String[] splitWords(String string) {
        if (string.isEmpty()) {
            return new String[] {};
        } else {
            return string.split("[ .:,;\t\n_()]+");
        }
    }

    public static List<Sequences<String>> slice(String... choices) {
        return slice(Arrays.asList(choices));
    }

    public static List<Sequences<String>> slice(List<String> choices) {
        Sequences<String> sequences = new Sequences<>(choices.size());
        for (String choice : choices) {
            sequences.add(new Sequence<>(splitWords(choice)));
        }
        return slice(sequences);
    }

    public static List<Sequences<String>> slice(Sequences<String> choices) {
        List<Sequences<String>> slices = new ArrayList<>();

        Sequence<String> commonStart = choices.commonStart();
        if (!commonStart.isEmpty()) {
            slices.add(new Sequences<>(commonStart));
        }
        Sequences<String> remainder = commonStart.isEmpty() ? choices : choices.removeIncluding(commonStart);

        while (remainder.maxLength() > 0) {
            Sequence<String> commonMiddle = remainder.commonMiddle();
            if (!commonMiddle.isEmpty()) {
                Sequences<String> gap = remainder.removeFrom(commonMiddle);
                slices.add(gap);
                slices.add(new Sequences<>(commonMiddle));
            }

            if (commonMiddle.isEmpty()) {
                slices.add(remainder);
                break;
            }
            remainder = remainder.removeIncluding(commonMiddle);
        }
        return slices;
    }

    public static <T> int max(List<? extends List<? extends T>> choices) {
        Optional<? extends List<? extends T>> reduced = choices.stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

}
