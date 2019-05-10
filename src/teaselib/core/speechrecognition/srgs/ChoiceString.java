package teaselib.core.speechrecognition.srgs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ChoiceString {
    final String phrase;
    final int choice;

    ChoiceString(String phrase, int choice) {
        super();
        this.phrase = phrase;
        this.choice = choice;
    }

    public boolean equalsIgnoreCase(ChoiceString anotherChoiceString) {
        return phrase.equalsIgnoreCase(anotherChoiceString.phrase);
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
            return Arrays.stream(words).map(word -> new ChoiceString(word, choice)).collect(Collectors.toList());
        }
    }

}