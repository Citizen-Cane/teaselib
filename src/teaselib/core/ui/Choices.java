package teaselib.core.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.motiondetection.Gesture;

public class Choices extends ArrayList<Choice> {
    private static final long serialVersionUID = 1L;

    public List<String> toText() {
        return stream().map(choice -> choice.text).collect(Collectors.toList());
    }

    public List<String> toDisplay() {
        return stream().map(choice -> choice.display).collect(Collectors.toList());
    }

    public List<Gesture> toGestures() {
        return stream().map(choice -> choice.gesture).collect(Collectors.toList());
    }

    public Choices(List<Choice> c) {
        super(c);
    }

    public Choices(int size) {
        super(size);
    }
}