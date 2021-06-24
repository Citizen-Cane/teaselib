package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;
import teaselib.core.speechrecognition.srgs.Sequences.SliceInProgress;

public class SlicedPhrases<T> implements Iterable<Sequences<T>> {
    static class Rating<T> {
        final Set<T> symbols;
        int duplicatedSymbols = 0;
        int maxCommonness = 0;

        Rating(Comparator<T> comparator) {
            this.symbols = new TreeSet<>(comparator);
        }

        Rating(Rating<T> other, Comparator<T> comparator) {
            this.symbols = new TreeSet<>(comparator);
            this.symbols.addAll(other.symbols);
            this.duplicatedSymbols = other.duplicatedSymbols;
            this.maxCommonness = other.maxCommonness;
        }

        void update(Sequences<T> slice) {
            slice.stream().forEach(this::update);
            maxCommonness = Math.max(maxCommonness, slice.maxCommonness());
        }

        public void update(Sequence<T> sequence) {
            for (T symbol : sequence) {
                if (symbols.contains(symbol)) {
                    duplicatedSymbols++;
                } else {
                    symbols.add(symbol);
                }
            }
        }

        public void updateMaxCommonness(Sequences<T> slice) {
            maxCommonness = Math.max(maxCommonness, slice.maxCommonness());
        }

        public void updateMaxCommonness(Sequence<T> sequence) {
            maxCommonness = Math.max(maxCommonness, sequence.maxCommonness());
        }

        @Override
        public String toString() {
            var rating = new StringBuilder();
            rating.append("Rating=[Cmax=");
            rating.append(maxCommonness);
            rating.append(" ");
            rating.append("distinct=");
            rating.append(symbols.size());
            rating.append(" ");
            rating.append("duplicates=");
            rating.append(duplicatedSymbols);
            rating.append("\n");
            rating.append("\t" + symbols);
            rating.append("]");
            return rating.toString();
        }

        public void invalidate() {
            symbols.clear();
            duplicatedSymbols = Integer.MAX_VALUE;
            maxCommonness = Integer.MIN_VALUE;
        }

        public boolean isInvalidated() {
            return maxCommonness < 0;
        }
    }

    private final List<Sequences<T>> elements;
    final Rating<T> rating;
    private final Function<Sequences<T>, String> toString;

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases) {
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        slice(candidates, withJoinedDuplicates(phrases), Objects::toString);
        return reduceNullRules(candidates.getResult());
    }

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases, Function<Sequences<T>, String> toString) {
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        slice(candidates, withJoinedDuplicates(phrases), toString);
        return reduceNullRules(candidates.getResult());
    }

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases, List<SlicedPhrases<T>> results,
            Function<Sequences<T>, String> toString) {
        slice(results, withJoinedDuplicates(phrases), toString);
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        results.removeIf(candidate -> candidate.rating.isInvalidated());
        results.stream().forEach(candidates::add);
        return reduceNullRules(candidates.getResult());
    }

    static <T> Sequences<T> withJoinedDuplicates(Sequences<T> sequences) {

        while (true) {
            Set<String> symbols = new TreeSet<>(String::compareToIgnoreCase);
            symbols.addAll(sequences.stream().flatMap(Sequence::stream).map(Objects::toString).collect(toSet()));
            Map<String, String> used = new TreeMap<>(String::compareToIgnoreCase);
            ignoreLastSymbols(sequences, symbols, used);
            removeNonJoinable(sequences, symbols, used);
            joinRemaining(sequences, symbols);
            if (symbols.isEmpty()) {
                break;
            }
        }

        return sequences;
    }

    private static <T> void ignoreLastSymbols(Sequences<T> sequences, Set<String> symbols, Map<String, String> used) {
        // remove last symbol in each sequence, since last symbols don't have next symbols
        for (Sequence<T> sequence : sequences) {
            var key = sequence.get(sequence.size() - 1).toString();
            if (symbols.contains(key)) {
                symbols.remove(key);
                used.put(key, "");
            }
        }
    }

    private static <T> void removeNonJoinable(Sequences<T> sequences, Set<String> symbols, Map<String, String> used) {
        Map<String, String> matches = new TreeMap<>(String::compareToIgnoreCase);

        // Ignore first symbols since they don't have previous symbols
        for (Sequence<T> sequence : sequences) {
            used.put(sequence.get(0).toString(), "");
        }

        for (Sequence<T> sequence : sequences) {
            for (int i = 0; i < sequence.size() - 1; i++) {
                var t = sequences.traits.joinSequenceOperator.apply(sequence.subList(i, i + 2)).toString();
                var first = sequence.get(i).toString();
                var second = sequence.get(i + 1).toString();
                if (symbols.contains(first)) {
                    var existing = matches.get(first);
                    if (existing == null) {
                        var usedSymbol = used.get(second);
                        if (usedSymbol == null) {
                            matches.put(first, t);
                            used.put(second, first);
                        } else if (!usedSymbol.equalsIgnoreCase(first)) {
                            remove(symbols, matches, first, usedSymbol);
                        }
                    } else if (!existing.equalsIgnoreCase(t)) {
                        symbols.remove(first);
                        matches.remove(first);
                    } else {
                        String usedSymbol = used.get(second);
                        if (usedSymbol != null && !usedSymbol.equalsIgnoreCase(first)) {
                            remove(symbols, matches, first, usedSymbol);
                        }
                    }
                } else if (used.containsKey(second)) {
                    remove(symbols, matches, first, second);
                } else if (used.containsKey(first)) {
                    // prepare to ignore matches that are successors of previously ignoreed symbols
                    matches.put(first, second);
                    used.put(second, first);
                }
            }
        }

        // Remove successors of previously ignored symbols
        for (Entry<String, String> entry : used.entrySet()) {
            if (entry.getValue().isBlank()) {
                symbols.remove(entry.getKey());
            }
        }
    }

    private static <T> void joinRemaining(Sequences<T> sequences, Set<String> symbols) {
        for (String symbol : symbols) {
            for (Sequence<T> sequence : sequences) {
                for (int i = 0; i < sequence.size() - 1; i++) {
                    var t = sequence.get(i).toString();
                    if (t.equalsIgnoreCase(symbol)) {
                        List<T> elements = sequence.subList(i, i + 2);
                        T joined = sequences.traits.joinSequenceOperator.apply(elements);
                        sequence.set(i, joined);
                        sequence.remove(i + 1);
                    }
                }
            }
        }
    }

    private static void remove(Set<String> symbols, Map<String, String> matches, String symbol, String usedSymbol) {
        symbols.remove(symbol);
        matches.remove(symbol);
        symbols.remove(usedSymbol);
        matches.remove(usedSymbol);
    }

    static <T> void slice(List<SlicedPhrases<T>> candidates, Sequences<T> sequences,
            Function<Sequences<T>, String> toString) {
        List<SliceInProgress<T>> more = sequences.sliceAll(candidates, new SlicedPhrases<>(sequences.traits, toString));
        while (!more.isEmpty()) {
            ArrayList<Sequences.SliceInProgress<T>> evenMore = new ArrayList<>();
            for (SliceInProgress<T> sliceInProgress : more) {
                evenMore.addAll(sliceInProgress.unsliced.sliceAll(candidates, sliceInProgress.soFar));
            }
            more = evenMore;
        }
    }

    private static <T> SlicedPhrases<T> leastDuplicatedSymbols(SlicedPhrases<T> a, SlicedPhrases<T> b) {
        long dA = a.rating.duplicatedSymbols;
        long dB = b.rating.duplicatedSymbols;
        if (dA == dB) {
            int sizeA = a.size();
            int sizeB = b.size();
            if (sizeA == sizeB) {
                return a.rating.maxCommonness > b.rating.maxCommonness ? a : b;
            } else {
                return sizeA < sizeB ? a : b;
            }
        } else {
            return dA < dB ? a : b;
        }
    }

    private static <T> SlicedPhrases<T> reduceNullRules(SlicedPhrases<T> result) {
        List<Sequences<T>> slices = result.elements;
        int size = slices.size();
        for (int i = 0; i < size - 1; i++) {
            Sequences<T> slice = slices.get(i);
            for (int k = 0; k < slice.size(); k++) {
                Sequence<T> sequence = slice.get(k);
                Sequences<T> next = slices.get(i + 1);
                if (next.isJoinableWith(sequence)) {
                    if (sequence.size() == 1) {
                        List<T> elements = sequence.traits.splitter.apply(sequence.get(0));
                        if (elements.size() > 1) {
                            sequence.set(0, elements.get(0));
                            var moved = elements.subList(1, elements.size());
                            next.add(new Sequence<>(moved, sequence.traits));
                        }
                    } else if (sequence.size() > 1) {
                        T remaining = sequence.remove(0);
                        Sequence<T> reduced = new Sequence<>(remaining, sequence.traits);
                        Sequence<T> moved = slice.set(k, reduced);
                        if (!join(moved, next, result)) {
                            next.add(moved);
                        }
                        if (join(reduced, slice, result)) {
                            slice.remove(reduced);
                            k--;
                        }
                    }
                }
            }
        }
        return result;

    }

    private static <T> boolean join(Sequence<T> reduced, Sequences<T> slice, SlicedPhrases<T> result) {
        boolean joined = false;
        for (int l = 0; l < slice.size(); l++) {
            Sequence<T> s = slice.get(l);
            if (s != reduced && s.compareTo(reduced) == 0 && s.joinableSequences(reduced)) {
                for (int m = 0; m < s.size(); m++) {
                    s.set(m, reduced.traits.joinCommonOperator.apply(asList(s.get(m), reduced.get(m))));
                }
                result.rating.updateMaxCommonness(s);
                result.rating.duplicatedSymbols--;
                joined = true;
                break;
            }
        }
        return joined;
    }

    public SlicedPhrases(Sequence.Traits<T> traits, Function<Sequences<T>, String> toString) {
        this.elements = new ArrayList<>();
        this.rating = new Rating<>(traits.comparator);
        this.toString = toString;
    }

    private SlicedPhrases(List<Sequences<T>> elements, Rating<T> rating, Sequence.Traits<T> traits,
            Function<Sequences<T>, String> toString) {
        this.elements = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Sequences<T> sequences = elements.get(i);
            this.elements.add(new Sequences<>(sequences));
        }
        this.rating = new Rating<>(rating, traits.comparator);
        this.toString = toString;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public int size() {
        return elements.size();
    }

    public Sequences<T> get(int i) {
        return elements.get(i);
    }

    public Stream<Sequences<T>> stream() {
        return elements.stream();
    }

    public SlicedPhrases<T> clone(Sequence.Traits<T> traits) {
        return new SlicedPhrases<>(elements, rating, traits, toString);
    }

    public int maxCommonness() {
        return elements.stream().map(Sequences::maxCommonness).reduce(Math::max).orElse(0);
    }

    public long distinctSymbolsCount() {
        if (elements.isEmpty()) {
            return 0;
        } else {
            Traits<T> traits = elements.get(0).traits;
            return elements.stream().flatMap(Sequences::stream).flatMap(Sequence::stream).map(traits.splitter::apply)
                    .flatMap(List::stream).map(T::toString).map(String::toLowerCase).distinct().count();
        }
    }

    public long symbolCount() {
        return elements.stream().collect(Collectors.summingLong(Sequences::symbolCount));
    }

    public long duplicatedSymbolsCount() {
        long symbols = symbolCount();
        long distinct = distinctSymbolsCount();
        return symbols - distinct;
    }

    public void addCompact(Sequences<T> slice) {
        if (!elements.isEmpty()) {
            move(slice);
        }

        if (!slice.isEmpty()) {
            rating.update(slice);
            elements.add(slice);
        }
    }

    private void move(Sequences<T> slice) {
        for (Sequence<T> sequence : new ArrayList<>(slice)) {
            boolean symbolsAppended = false;
            Sequence<T> newSymbols = sequence;
            Sequences<T> sourceSlice = slice;

            for (int j = elements.size() - 1; j >= 0; j--) {
                Sequences<T> targetSlice = elements.get(j);
                boolean moveableSequence = true;
                Sequence<T> mergeableSequence = null;
                int targetSliceSize = targetSlice.size();
                for (int k = 0; k < targetSliceSize; k++) {
                    Sequence<T> targetSequence = targetSlice.get(k);
                    if (sequence.joinableSequences(targetSequence)) {
                        // merge sequence into target
                        if (sequence.compareTo(targetSequence) == 0) {
                            mergeableSequence = targetSequence;
                        }
                    } else if (sequence.joinablePhrase(targetSequence)) {
                        // Append sequence to target sequence
                        sourceSlice.remove(sequence);
                        targetSequence.addAll(sequence);
                        symbolsAppended = true;
                        moveableSequence = false;
                        break;
                    } else {
                        moveableSequence = false;
                    }
                }

                if (moveableSequence) {
                    if (mergeableSequence != null) {
                        sourceSlice.remove(sequence);
                        // merge sequence into target
                        int size = sequence.size();
                        for (int i = 0; i < size; i++) {
                            mergeableSequence.set(i, targetSlice.traits.joinCommonOperator
                                    .apply(asList(sequence.get(i), mergeableSequence.get(i))));
                        }
                        rating.updateMaxCommonness(mergeableSequence);
                        sequence = mergeableSequence;
                        symbolsAppended = false;
                        break;
                    } else {
                        // Move sequence
                        sourceSlice.remove(sequence);
                        targetSlice.add(sequence);
                        symbolsAppended = true;
                        sourceSlice = targetSlice;
                    }
                } else {
                    break;
                }
            }

            if (symbolsAppended) {
                rating.update(newSymbols);
                rating.updateMaxCommonness(newSymbols);
            }
        }

    }

    public List<T> complete(T text) {
        return elements.stream()
                .map(sequences -> sequences.stream()
                        .filter(sequence -> sequences.traits.intersectionPredicate.test(sequence.get(0), text))
                        .reduce(Sequence::maxLength).orElse(new Sequence<>(sequences.traits)))
                .flatMap(Sequence::stream).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (rating.isInvalidated()) {
            return "dropped";
        } else {
            var phrases = new StringBuilder();
            phrases.append(rating.toString());
            phrases.append("\n");
            phrases.append("Cmax=");
            phrases.append(maxCommonness());
            phrases.append(" ");
            phrases.append(" ");
            phrases.append("distinct=");
            phrases.append(distinctSymbolsCount());
            phrases.append(" ");
            phrases.append("duplicates=");
            phrases.append(duplicatedSymbolsCount());
            phrases.append("\n");

            phrases.append(stream().map(toString).collect(Collectors.joining("\n")));
            return phrases.toString();
        }
    }

    public boolean worseThan(List<SlicedPhrases<T>> candidates) {
        if (candidates.isEmpty()) {
            return false;
        } else {
            return SlicedPhrases.leastDuplicatedSymbols(this, candidates.get(0)) != this;
        }
    }

    public void drop() {
        elements.clear();
        rating.invalidate();
    }

    @Override
    public Iterator<Sequences<T>> iterator() {
        return elements.iterator();
    }

}
