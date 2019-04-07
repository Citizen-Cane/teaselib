package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.Answer;

public class Choice {
    public final Answer answer;
    public final String display;
    public final List<String> phrases;

    public static String getDisplay(Choice choice) {
        return choice.display;
    }

    public Choice(Answer answer, String display, List<String> phrases) {
        this.answer = answer;
        this.display = display;
        this.phrases = phrases;
    }

    public Choice(Answer answer) {
        this(answer, answer.text.get(0), answer.text);
    }

    public Choice(Answer answer, String display) {
        this(answer, display, Collections.singletonList(display));
    }

    public Choice(String text, String display, String... phrases) {
        this(Answer.resume(text), display, Arrays.asList(phrases));
    }

    public Choice(String text, String display) {
        this(Answer.resume(text), display);
    }

    public Choice(String text) {
        this(text, text);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((answer == null) ? 0 : answer.hashCode());
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        result = prime * result + ((phrases == null) ? 0 : phrases.hashCode());
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
        Choice other = (Choice) obj;
        if (answer == null) {
            if (other.answer != null)
                return false;
        } else if (!answer.equals(other.answer))
            return false;
        if (display == null) {
            if (other.display != null)
                return false;
        } else if (!display.equals(other.display))
            return false;
        if (phrases == null) {
            if (other.phrases != null)
                return false;
        } else if (!phrases.equals(other.phrases))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Answer=" + answer + " display='" + display + "' phrases=" + phrases;
    }
}
