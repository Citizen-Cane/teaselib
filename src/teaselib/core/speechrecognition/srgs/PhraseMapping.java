package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

import java.util.List;
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

    public abstract Set<Integer> srgs(Set<Integer> phrases);

    public abstract boolean isOptional(PhraseString phrase, Set<Integer> uncovered);

    public static class Strict extends PhraseMapping {
        public Strict(Choices choices) {
            super(choices);
        }

        @Override
        public int choice(int index) {
            return phrase2choice.get(index);
        }

        @Override
        public Set<Integer> srgs(Set<Integer> indices) {
            return indices;
        }

        @Override
        public boolean isOptional(PhraseString phrase, Set<Integer> uncovered) {
            return false;
        }

    }

    public static class Relaxed extends PhraseMapping {

        public Relaxed(Choices choices) {
            super(choices);
        }

        @Override
        public int choice(int index) {
            return phrase2choice.get(index);
        }

        @Override
        public Set<Integer> srgs(Set<Integer> indices) {
            return indices;
        }

        @Override
        public boolean isOptional(PhraseString phrase, Set<Integer> uncovered) {
            return !uncovered.isEmpty() && choices(phrase.indices).equals(choices(uncovered))
                    && phrase.indices.size() + uncovered.size() == phrase2choice.size();

        }

        private Set<Integer> choices(Set<Integer> indices) {
            return indices.stream().map(phrase2choice::get).collect(toSet());
        }

    }

}
