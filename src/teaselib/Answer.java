package teaselib;

import java.util.Arrays;
import java.util.List;

/**
 * Answers define choices for input methods. The first text is the dominant phrase - it's used for display input methods
 * - so it should be the longest and most detailed.
 * <p>
 * Users will tend to abbreviate, so abbreviations of the phrase should come last. On the other hand, while chatting
 * displaying shorter phrases may be more appropriate.
 *
 * @author Citizen-Cane
 *
 */
public class Answer {

    public enum Meaning {
        YES,
        NO,
        RESUME,
        TIMEOUT
    }

    public static class Yes extends Answer {

        public Yes(List<String> text) {
            super(Meaning.YES, text);
        }

        public Yes(String... text) {
            super(Meaning.YES, text);
        }

    }

    public static class No extends Answer {

        public No(List<String> text) {
            super(Meaning.NO, text);
        }

        public No(String... text) {
            super(Meaning.NO, text);
        }

    }

    /**
     * Returned when a script function has timed out.
     */
    public static final Answer Timeout = new Answer(Meaning.TIMEOUT, ScriptFunction.TimeoutString);

    public final Meaning meaning;
    public final List<String> text;

    public static Answer.Yes yes(String... text) {
        return yes(Arrays.asList(text));
    }

    public static Answer.Yes yes(List<String> text) {
        return new Answer.Yes(text);
    }

    public static Answer.No no(String... text) {
        return no(Arrays.asList(text));
    }

    public static Answer.No no(List<String> text) {
        return new Answer.No(text);
    }

    public static Answer resume(String... text) {
        return resume(Arrays.asList(text));
    }

    public static Answer resume(List<String> text) {
        return new Answer(Meaning.RESUME, text);
    }

    public Answer(Meaning meaning, String... text) {
        this(meaning, Arrays.asList(text));
    }

    public Answer(Meaning meaning, List<String> text) {
        this.meaning = meaning;
        this.text = text;
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
