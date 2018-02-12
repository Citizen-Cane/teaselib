package teaselib;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Answer {
    public enum Meaning {
        YES,
        NO,
        INDIFFERENT
    }

    public final String text;
    public final Meaning meaning;

    public static Answer yes(String text) {
        return new Answer(text, Meaning.YES);
    }

    public static Answer no(String text) {
        return new Answer(text, Meaning.NO);
    }

    public static List<Answer> all(List<String> answers) {
        List<Answer> all = new ArrayList<>(answers.size());
        for (String answer : answers) {
            all.add(Answer.indifferent(answer));
        }
        return all;
    }

    public static List<Answer> all(String... answers) {
        List<Answer> all = new ArrayList<>(answers.length);
        for (String answer : answers) {
            all.add(Answer.indifferent(answer));
        }
        return all;
    }

    public static Answer indifferent(String text) {
        return new Answer(text, Meaning.INDIFFERENT);
    }

    public Answer(String text, Meaning meaning) {
        this.text = text;
        this.meaning = meaning;
    }
}
