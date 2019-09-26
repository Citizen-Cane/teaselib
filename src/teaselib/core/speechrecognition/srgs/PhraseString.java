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

class PhraseString {
    final String phrase;
    final Set<Integer> indices;

    PhraseString(String phrase, int indices) {
        super();
        this.phrase = phrase;
        this.indices = Collections.singleton(indices);
    }

    PhraseString(String phrase, Set<Integer> indices) {
        super();
        this.phrase = phrase;
        this.indices = indices;
    }

    @Override
    public String toString() {
        return phrase;
    }

    public boolean samePhrase(PhraseString other) {
        return phrase.equalsIgnoreCase(other.phrase);
    }

    public List<PhraseString> words() {
        if (phrase.isEmpty()) {
            return singletonList(this);
        } else {
            String[] words = phrase.split("[ .:,;\t\n_()]+");
            return stream(words).map(word -> new PhraseString(word, indices)).collect(toList());
        }
    }

    public static PhraseString joinCommon(List<PhraseString> strings) {
        if (strings.isEmpty()) {
            throw new NoSuchElementException();
        }

        Set<Integer> results = strings.stream().flatMap(phrase -> phrase.indices.stream()).collect(toSet());
        return new PhraseString(strings.get(0).phrase, results);
    }

    public static PhraseString joinSequence(List<PhraseString> strings) {
        if (strings.isEmpty()) {
            throw new NoSuchElementException("Empty phrases cannot be concatenated since they don't provide indices");
        }

        Set<Integer> choice = strings.stream().map(phrase -> phrase.indices).reduce(PhraseString::intersect)
                .orElseThrow();
        return new PhraseString(strings.stream().map(element -> element.phrase).collect(joining(" ")).trim(), choice);
    }

    private static <T> Set<T> intersect(Set<T> a, Set<T> b) {
        return a.stream().filter(b::contains).collect(Collectors.toSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(indices, phrase);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PhraseString))
            return false;
        PhraseString other = (PhraseString) obj;
        return Objects.equals(indices, other.indices) && samePhrase(other);
    }

}