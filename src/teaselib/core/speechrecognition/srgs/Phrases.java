package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.List;

public class Phrases extends ArrayList<Sequences<String>> {
    private static final long serialVersionUID = 1L;

    public static Phrases of(List<String> choices) {
        return new Phrases(SequenceUtil.slice(choices));
    }

    public Phrases(List<Sequences<String>> phrases) {
        super(phrases);
    }

    public Sequences<String> flatten() {
        return Sequences.flatten(this);
    }
}
