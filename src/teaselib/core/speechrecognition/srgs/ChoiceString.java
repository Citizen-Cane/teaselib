package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
            return Collections.emptyList();
        } else {
            String[] words = phrase.split("[ .:,;\t\n_()]+");
            List<ChoiceString> collect = Arrays.stream(words).map(word -> new ChoiceString(word, choice))
                    .collect(Collectors.toList());
            collect.add(new ChoiceString("", choice));
            return collect;
        }
    }

    public static ChoiceString joinCommon(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        // TODO Assumes all phrases are the same - evaluate
        // TODO Make choice index a list, join indices, decide later about common rules

        List<Integer> results = choices.stream().map(phrase -> phrase.choice).distinct().collect(Collectors.toList());
        int choice = results.size() == 1 ? results.get(0) : Phrases.COMMON_RULE;
        return new ChoiceString(choices.get(0).phrase, choice);
    }

    public static ChoiceString joinSequence(List<ChoiceString> choices) {
        if (choices.isEmpty()) {
            throw new NoSuchElementException();
        }

        Integer choice = choices.stream().map(phrase -> phrase.choice).reduce(Math::max).orElseThrow();
        return new ChoiceString(choices.stream().map(element -> element.phrase).collect(Collectors.joining(" ")).trim(),
                choice);
    }

    public static ChoiceString concat(ChoiceString a, ChoiceString b) {
        return new ChoiceString(String.join(" ", a.phrase, b.phrase).trim(), Math.max(a.choice, b.choice));
    }

}