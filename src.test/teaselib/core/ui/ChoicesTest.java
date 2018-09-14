package teaselib.core.ui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import teaselib.motiondetection.Gesture;

public class ChoicesTest {
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateText() {
        new Choices(Arrays.asList(new Choice(Gesture.Nod, "Yes", "Yes1"), new Choice(Gesture.None, "Yes", "Yes2")));
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateDisplay() {
        new Choices(Arrays.asList(new Choice(Gesture.Nod, "Yes1", "Yes"), new Choice(Gesture.None, "Yes2", "Yes")));
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateGestures() {
        new Choices(Arrays.asList(new Choice(Gesture.Nod, "Yes1", "Yes"), new Choice(Gesture.Nod, "Yes2", "Yes")));
    }

    @Test
    public void testThatGestureNoneCanOccurMultipleTimes() {
        List<Choice> answers = Arrays.asList(new Choice(Gesture.None, "Yes1", "Yes1"),
                new Choice(Gesture.None, "Yes2", "Yes2"));
        Choices choices = new Choices(answers);
        assertEquals(answers.size(), choices.size());
    }
}
