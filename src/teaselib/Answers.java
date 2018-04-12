package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Answers extends ArrayList<Answer> {
    private static final long serialVersionUID = 1L;

    public Answers(Answer... answers) {
        super(Arrays.asList(answers));
    }

    public Answers(Answer answer) {
        super(Arrays.asList(answer));
    }

    public Answers(List<Answer> answers) {
        super(answers);
    }

    public Answers(int size) {
        super(size);
    }

    /**
     * Searches all answers in the list for the given string.
     * 
     * @param answer
     *            The string to search for.
     * @return Returns the index of the {@link Answer} that contains the string, or -1 if not found.
     */
    public int indexOf(String answer) {
        int index = 0;
        for (Answer element : this) {
            if (element.text.equals(answer)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
    }

    protected static Answers of(List<String> text) {
        Answers answers = new Answers(text.size());
        for (String answer : text) {
            answers.add(Answer.resume(answer));
        }
        return answers;
    }

    protected static Answers of(String choice, String... more) {
        Answers answers = new Answers(1 + more.length);
        answers.add(Answer.resume(choice));
        for (String text : more) {
            answers.add(Answer.resume(text));
        }
        return answers;
    }

    public static Answers of(Answer answer, Answer... more) {
        Answers answers = new Answers(1 + more.length);
        answers.add(answer);
        answers.addAll(Arrays.asList(more));
        return answers;
    }
}