package teaselib.core.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import teaselib.Answer.Meaning;
import teaselib.core.speechrecognition.srgs.IndexMap;

/**
 * 
 * Choices group Answers to form a prompt. Each {@link teaselib.core.ui.Choice} contains display text and phrases with
 * text variables expanded.
 * 
 * @author Citizen-Cane
 *
 */
public class Choices extends ArrayList<Choice> {
    private static final long serialVersionUID = 1L;

    public final Locale locale;
    public final Intention intention;

    public List<String> toText() {
        return stream().map(choice -> choice.answer.text.get(0)).collect(Collectors.toList());
    }

    public List<String> toDisplay() {
        return stream().map(Choice::getDisplay).collect(Collectors.toList());
    }

    public List<List<String>> toPhrases() {
        return stream().map(choice -> choice.phrases).collect(Collectors.toList());
    }

    public Choices(Locale locale, Intention intention, Choice... choices) {
        this(locale, intention, Arrays.asList(choices));
    }

    public Choices(Locale locale, Intention intention, List<Choice> choices) {
        super(choices);
        this.locale = locale;
        this.intention = intention;
        checkForDuplicates(this);
    }

    public IntUnaryOperator indexMapper() {
        IndexMap<Integer> phraseToChoice = new IndexMap<>();
        stream().forEach(choice -> choice.phrases.stream().forEach(phrase -> phraseToChoice.add(indexOf(choice))));
        return phraseToChoice::get;
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

}