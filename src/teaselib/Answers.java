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
}