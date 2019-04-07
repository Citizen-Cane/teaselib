package teaselib;

import java.util.Collections;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Answer {
    public enum Meaning {
        YES,
        NO,
        RESUME
    }

    public final List<String> text;
    public final Meaning meaning;

    public static Answer yes(String text) {
        return new Answer(text, Meaning.YES);
    }

    public static Answer no(String text) {
        return new Answer(text, Meaning.NO);
    }

    public static Answer resume(String text) {
        return new Answer(text, Meaning.RESUME);
    }

    public Answer(String text, Meaning meaning) {
        this.text = Collections.singletonList(text);
        this.meaning = meaning;
    }

    @Override
    public String toString() {
        return meaning + " -> " + text;
    }

}
