package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

class PhraseString {
    static final Sequence.Traits<PhraseString> Traits = new Traits<>(PhraseString::samePhrase, PhraseString::words,
            PhraseString::commonness, PhraseString::joinCommon, PhraseString::joinSequence);

    final String phrase;
    final Set<Integer> indices;

    PhraseString(String phrase, int indices) {
        super();
        this.phrase = phrase;
        this.indices = singleton(indices);
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

    // TODO split collecting and computing the values
    // private static int commonness(List<List<PhraseString>> elements) {
    // return elements.stream().flatMap(List::stream).map(element -> (element).indices.size()).reduce(0,
    // (x, y) -> x + y - 1);
    // }
    private static int commonness(PhraseString element) {
        return element.indices.size() * element.words().size();
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
        return a.stream().filter(b::contains).collect(toSet());
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