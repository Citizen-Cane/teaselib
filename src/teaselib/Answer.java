package teaselib;

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

    public final String text;
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
        this.text = text;
        this.meaning = meaning;
    }

    @Override
    public String toString() {
        return meaning + " -> " + text;
    }

}
