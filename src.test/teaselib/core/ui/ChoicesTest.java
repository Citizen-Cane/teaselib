package teaselib.core.ui;


import org.junit.jupiter.api.Test;
import teaselib.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChoicesTest {
    @SuppressWarnings("unused")
    @Test
    public void testDuplicateText() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Choices(Locale.ENGLISH, Intention.Decide,
                    new Choice(Answer.yes("Yes"), "Yes1"),
                    new Choice(Answer.resume("Yes"), "Yes2"));
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void testDuplicateDisplay() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Choices(Locale.ENGLISH, Intention.Decide,
                    new Choice(Answer.yes("Yes1"), "Yes"),
                    new Choice(Answer.yes("Yes2"), "Yes"));
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void testDuplicateGestures() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Choices(Locale.ENGLISH, Intention.Decide,
                    new Choice(Answer.yes("Yes1"), "Yes"),
                    new Choice(Answer.yes("Yes2"), "Yes"));
        });
    }

    @Test
    public void testThatGestureNoneCanOccurMultipleTimes() {
        List<Choice> answers = Arrays.asList(new Choice(Answer.resume("Yes1"), "Yes1"),
                new Choice(Answer.resume("Yes2"), "Yes2"));
        Choices choices = new Choices(Locale.ENGLISH, Intention.Decide, answers);
        assertEquals(answers.size(), choices.size());
    }
}
