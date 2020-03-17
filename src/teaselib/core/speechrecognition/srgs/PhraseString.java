package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

class PhraseString {
    static final Sequence.Traits<PhraseString> Traits = new Traits<>(PhraseString::samePhrase, PhraseString::words,
            PhraseString::commonness, PhraseString::joinCommon, PhraseString::joinSequence,
            PhraseString::joinableSequences, PhraseString::joinablePhrases);

    final String phrase;
    final Set<Integer> indices;

    PhraseString(String phrase, int index) {
        this.phrase = phrase;
        this.indices = singleton(index);
    }

    PhraseString(String phrase, Integer... indices) {
        this.phrase = phrase;
        this.indices = new HashSet<>(Arrays.asList(indices));
    }

    PhraseString(String phrase, Set<Integer> indices) {
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

        Set<Integer> choice = strings.stream().map(phrase -> phrase.indices).reduce(PhraseString::intersection)
                .orElseThrow();
        return new PhraseString(strings.stream().map(element -> element.phrase).collect(joining(" ")).trim(), choice);
    }

    static boolean joinableSequences(PhraseString phrase, Collection<PhraseString> collection) {
        Set<Integer> collect = collection.stream().map(p -> p.indices).flatMap(Set::stream).collect(Collectors.toSet());
        return !PhraseString.intersect(phrase.indices, collect);
    }

    static boolean joinablePhrases(PhraseString phrase1, PhraseString phrase2) {
        return phrase1.indices.equals(phrase2.indices);
    }

    static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        return a.stream().filter(b::contains).collect(toSet());
    }

    static <T> boolean intersect(Set<T> a, Set<T> b) {
        return a.stream().anyMatch(b::contains);
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