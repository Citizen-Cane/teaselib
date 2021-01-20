package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import teaselib.core.ui.Choices;

public abstract class PhraseMapping {
    final IndexMap<Integer> phrase2choice;
    final List<PhraseString> phrases;

    PhraseMapping(Choices choices) {
        this.phrase2choice = new IndexMap<>();
        this.phrases = choices.stream()
                .flatMap(choice -> choice.phrases.stream()
                        .map(phrase -> new PhraseString(phrase, phrase2choice.add(choices.indexOf(choice)))))
                .collect(toList());
    }

    public abstract int choice(int phrase);

    public abstract Set<Integer> choices(Set<Integer> phrases);

    public abstract Set<Integer> phrases(int choice);

    static class Strict extends PhraseMapping {
        public Strict(Choices choices) {
            super(choices);
        }

        @Override
        public int choice(int index) {
            return phrase2choice.get(index);
        }

        @Override
        public Set<Integer> choices(Set<Integer> indices) {
            return indices;
        }

        @Override
        public Set<Integer> phrases(int index) {
            return Collections.singleton(index);
        }
    }

    static class Lax extends PhraseMapping {

        private final Map<Integer, Set<Integer>> choice2phrase;

        public Lax(Choices choices) {
            super(choices);
            this.choice2phrase = new HashMap<>();
            for (int i = 0; i < phrase2choice.size(); i++) {
                int choice = phrase2choice.get(i);
                choice2phrase.computeIfAbsent(choice, key -> new HashSet<>()).add(i);
            }
        }

        @Override
        public int choice(int index) {
            return index;
        }

        @Override
        public Set<Integer> choices(Set<Integer> indices) {
            return indices.stream().map(phrase2choice::get).collect(toSet());
        }

        @Override
        public Set<Integer> phrases(int index) {
            return choice2phrase.get(index);
        }

    }
}