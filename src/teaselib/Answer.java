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
