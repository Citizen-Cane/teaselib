package teaselib.core.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.motiondetection.Gesture;

public class Choices extends ArrayList<Choice> {
    private static final long serialVersionUID = 1L;

    public List<String> toText() {
        return stream().map(choice -> choice.text).collect(Collectors.toList());
    }

    public List<String> toDisplay() {
        return stream().map(Choice::getDisplay).collect(Collectors.toList());
    }

    public List<Gesture> toGestures() {
        return stream().map(choice -> choice.gesture).collect(Collectors.toList());
    }

    public List<List<String>> toPhrases() {
        for (Choice choice : this) {
            if (choice.phrases.size() != 1) {
                throw new IllegalArgumentException(choice.toString());
            }
        }
        return stream().map(choice -> choice.phrases).collect(Collectors.toList());
    }

    public Choices(Choice... choices) {
        this(Arrays.asList(choices));
    }

    public Choices(List<Choice> choices) {
        super(choices);
        checkForDuplicates(this);
    }

    public List<Choice> get(Prompt.Result result) {
        // TODO Multiple choices
        return Collections.singletonList(get(result.elements.get(0)));
    }

    private static void checkForDuplicates(Choices choices) {
        check(choices.toText(), "Duplicate result text");
        check(choices.toDisplay(), "Duplicate display texts");
        check(choices.toGestures().stream().filter(gesture -> gesture != Gesture.None).collect(Collectors.toList()),
                "Duplicate gestures");
    }

    private static <T> void check(List<T> choices, String message) {
        if (choices.size() > new HashSet<>(choices).size()) {
            throw new IllegalArgumentException(message + ": " + choices);
        }
    }

    public Choices(int size) {
        super(size);
    }
}