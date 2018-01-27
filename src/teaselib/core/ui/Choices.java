/**
 * 
 */
package teaselib.core.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.motiondetection.Gesture;

public class Choices extends ArrayList<Choice> {
    private static final long serialVersionUID = 1L;

    public static Choices from(List<String> choices) {
        return new Choices(choices.stream().map(Choice::new).collect(Collectors.toList()));
    }

    public static Choices from(Choice... choices) {
        return new Choices(Arrays.asList(choices));
    }

    public static Choices from(String text) {
        Choices choices = new Choices(1);
        choices.add(new Choice(text));
        return choices;
    }

    public List<String> toText() {
        return stream().map(choice -> choice.text).collect(Collectors.toList());
    }

    public List<String> toDisplay() {
        return stream().map(choice -> choice.display).collect(Collectors.toList());
    }

    public List<Gesture> toGestures() {
        return stream().map(choice -> choice.gesture).collect(Collectors.toList());
    }

    public Choices(Collection<Choice> c) {
        super(c);
    }

    public Choices(int size) {
        super(size);
    }

}