package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

public class PhraseString {
    public static final Sequence.Traits<PhraseString> Traits = new Traits<>(//
            PhraseString::compareTo, //
            PhraseString::words, //
            PhraseString::commonness, //
            PhraseString::joinCommon, //
            PhraseString::joinSequence, //
            PhraseString::joinableSequences, //
            PhraseString::joinablePhrases, //
            PhraseString::intersectionPredicate);

    public final String phrase;
    public final Set<Integer> indices;

    public PhraseString(String phrase, int index) {
        this.phrase = phrase;
        this.indices = singleton(index);
    }

    public PhraseString(String phrase, Integer... indices) {
        this.phrase = phrase;
        this.indices = new HashSet<>(Arrays.asList(indices));
    }

    public PhraseString(String phrase, Set<Integer> indices) {
        this.phrase = phrase;
        this.indices = indices;
    }

    @Override
    public String toString() {
        return phrase;
    }

    public int compareTo(PhraseString other) {
        return phrase.compareToIgnoreCase(other.phrase);
    }

    public List<PhraseString> words() {
        if (phrase.isEmpty()) {
            return singletonList(this);
        } else {
            String[] words = words(phrase);
            return stream(words).map(word -> new PhraseString(word, indices)).collect(toList());
        }
    }

    public List<String> split() {
        return Arrays.asList(words(phrase));
    }

    public static String[] words(String phrase) {
        return phrase.split("[ .:,;\t\n_()]+");
    }

    private static int commonness(PhraseString element) {
        return element.indices.size();
    }

    static PhraseString joinCommon(List<PhraseString> strings) {
        if (strings.isEmpty()) {
            throw new NoSuchElementException();
        }

        Set<Integer> results = strings.stream().flatMap(phrase -> phrase.indices.stream()).collect(toSet());
        return new PhraseString(strings.get(0).phrase, results);
    }

    static PhraseString joinSequence(List<PhraseString> strings) {
        if (strings.isEmpty()) {
            throw new NoSuchElementException("Empty phrases cannot be concatenated since they don't provide indices");
        }

        Set<Integer> choice = strings.stream().map(phrase -> phrase.indices).reduce(PhraseString::intersection)
                .orElseThrow();
        return new PhraseString(strings.stream().map(element -> element.phrase).collect(joining(" ")), choice);
    }

    static boolean joinableSequences(List<PhraseString> sequence1, List<PhraseString> sequence2) {
        Set<Integer> indices1 = sequence1.size() == 1 ? sequence1.get(0).indices
                : sequence1.stream().map(p -> p.indices).flatMap(Set::stream).collect(Collectors.toSet());
        Set<Integer> indices2 = sequence2.size() == 1 ? sequence2.get(0).indices
                : sequence2.stream().map(p -> p.indices).flatMap(Set::stream).collect(Collectors.toSet());
        return !PhraseString.intersect(indices1, indices2);
    }

    static boolean joinablePhrases(List<PhraseString> phrases1, List<PhraseString> phrases2) {
        return phrases1.get(0).indices.equals(phrases2.get(0).indices);
    }

    private static boolean joinablePhrases(PhraseString phrase1, PhraseString phrase2) {
        return phrase1.indices.equals(phrase2.indices);
    }

    public static boolean intersectionPredicate(PhraseString a, PhraseString b) {
        return intersect(a.indices, b.indices);
    }

    public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        return a.stream().filter(b::contains).collect(toSet());
    }

    public static <T> boolean intersect(Set<T> a, Set<T> b) {
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
        return joinablePhrases(this, other) && compareTo(other) == 0;
    }

}