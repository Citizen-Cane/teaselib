package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ChoiceString {
    final String phrase;
    final Set<Integer> choices;

    ChoiceString(String phrase, int choice) {
        super();
        this.phrase = phrase;
        this.choices = Collections.singleton(choice);
    }

    ChoiceString(String phrase, Set<Integer> choices) {
        super();
        this.phrase = phrase;
        this.choices = choices;
    }

    @Override
    public String toString() {
        return phrase;
    }

    public boolean samePhrase(ChoiceString other) {
        return phrase.equalsIgnoreCase(other.phrase);
    }

    public List<ChoiceString> words() {
        if (phrase.isEmpty()) {
            return singletonList(this);
        } else {
            String[] words = phrase.split("[ .:,;\t\n_()]+");
            return stream(words).map(word -> new ChoiceString(word, choices)).collect(toList());
        }
    }

    public static ChoiceString joinCommon(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        Set<Integer> results = choices.stream().flatMap(phrase -> phrase.choices.stream()).collect(toSet());
        return new ChoiceString(choices.get(0).phrase, results);
    }

    public static ChoiceString joinSequence(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        Set<Integer> choice = choices.stream().map(phrase -> phrase.choices).reduce(ChoiceString::intersect)
                .orElseThrow();
        return new ChoiceString(choices.stream().map(element -> element.phrase).collect(joining(" ")).trim(), choice);
    }

    private static <T> Set<T> intersect(Set<T> a, Set<T> b) {
        return a.stream().filter(b::contains).collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(choices, phrase);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ChoiceString))
            return false;
        ChoiceString other = (ChoiceString) obj;
        return Objects.equals(choices, other.choices) && samePhrase(other);
    }

}