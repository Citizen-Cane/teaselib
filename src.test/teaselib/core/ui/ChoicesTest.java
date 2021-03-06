package teaselib.core.ui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.Answer;

public class ChoicesTest {
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateText() {
        new Choices(Arrays.asList(new Choice(Answer.yes("Yes"), "Yes1"), new Choice(Answer.resume("Yes"), "Yes2")));
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateDisplay() {
        new Choices(Arrays.asList(new Choice(Answer.yes("Yes1"), "Yes"), new Choice(Answer.yes("Yes2"), "Yes")));
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateGestures() {
        new Choices(Arrays.asList(new Choice(Answer.yes("Yes1"), "Yes"), new Choice(Answer.yes("Yes2"), "Yes")));
    }

    @Test
    public void testThatGestureNoneCanOccurMultipleTimes() {
        List<Choice> answers = Arrays.asList(new Choice(Answer.resume("Yes1"), "Yes1"),
                new Choice(Answer.resume("Yes2"), "Yes2"));
        Choices choices = new Choices(answers);
        assertEquals(answers.size(), choices.size());
    }
}
