package teaselib.core.ui;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import teaselib.Answer.Meaning;

public class Choices extends ArrayList<Choice> {
    private static final long serialVersionUID = 1L;

    public List<String> toText() {
        return stream().map(choice -> choice.answer.text.get(0)).collect(Collectors.toList());
    }

    public List<String> toDisplay() {
        return stream().map(Choice::getDisplay).collect(Collectors.toList());
    }

    public List<List<String>> toPhrases() {
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
        check(choices.stream().flatMap(choice -> choice.answer.text.stream()).collect(Collectors.toList()),
                "Duplicate result text");
        check(choices.toDisplay(), "Duplicate display texts");
        check(choices.stream().filter(choice -> choice.answer.meaning != Meaning.RESUME).collect(Collectors.toList()),
                "Duplicate gestures");
    }

    private static <T> void check(List<T> choices, String message) {
        if (choices.size() > new HashSet<>(choices).size()) {
            throw new IllegalArgumentException(message + ": " + choices);
        }
    }

    public List<String> firstPhraseOfEach() {
        return stream().map(choice -> choice.phrases.get(0)).collect(toList());
    }

}