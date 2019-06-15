package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

class ChoiceString {
    final String phrase;
    final int choice;

    ChoiceString(String phrase, int choice) {
        super();
        this.phrase = phrase;
        this.choice = choice;
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
            return Collections.singletonList(this);
        } else {
            String[] words = phrase.split("[ .:,;\t\n_()]+");
            List<ChoiceString> collect = stream(words).map(word -> new ChoiceString(word, choice)).collect(toList());
            collect.add(new ChoiceString("", choice));
            return collect;
        }
    }

    public static ChoiceString joinCommon(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        Set<Integer> results = choices.stream().map(phrase -> phrase.choice).collect(toSet());
        int choice;
        if (results.size() == 1) {
            choice = results.iterator().next();
        } else if (results.size() == 2 && results.contains(Phrases.COMMON_RULE)) {
            choice = results.stream().filter(i -> i != Phrases.COMMON_RULE).reduce(Math::max).orElseThrow();
        } else {
            choice = Phrases.COMMON_RULE;
        }
        return new ChoiceString(choices.get(0).phrase, choice);
    }

    public static ChoiceString joinSequence(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        Integer choice = choices.stream().map(phrase -> phrase.choice).reduce(Math::max).orElseThrow();
        return new ChoiceString(choices.stream().map(element -> element.phrase).collect(joining(" ")).trim(), choice);
    }

}