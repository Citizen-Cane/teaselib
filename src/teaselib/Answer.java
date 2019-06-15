package teaselib;

import java.util.Arrays;
import java.util.List;

/**
 * Answers are used to build choices for input methods. The first text is the dominant phrase - it's used for display
 * input methods - so it should be the longest and most detailed. Users will tend to abbreviate, so shorter phrases
 * should come last.
 * <p>
 * On the other hand, while chatting shorter phrases may be more appropriate, and the shorter phrase comes first.
 * <p>
 * TODO Improve display to slice phrases and display all groups of an answer.
 * 
 * @author Citizen-Cane
 *
 */
public class Answer {
    public enum Meaning {
        YES,
        NO,
        RESUME
    }

    public final Meaning meaning;
    public final List<String> text;

    public static Answer yes(String... text) {
        return new Answer(Meaning.YES, text);
    }

    public static Answer no(String... text) {
        return new Answer(Meaning.NO, text);
    }

    public static Answer resume(String... text) {
        return new Answer(Meaning.RESUME, text);
    }

    public Answer(Meaning meaning, String... text) {
        this.meaning = meaning;
        this.text = Arrays.asList(text);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((meaning == null) ? 0 : meaning.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Answer other = (Answer) obj;
        if (meaning != other.meaning)
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return meaning + " -> " + text;
    }

}
