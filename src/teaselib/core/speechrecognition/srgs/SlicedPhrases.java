package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;
import teaselib.core.speechrecognition.srgs.Sequences.SliceInProgress;

public class SlicedPhrases<T> {
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
            maxCommonness = Math.max(maxCommonness, slice.max());
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
            maxCommonness = Math.max(maxCommonness, slice.max());
        }

        @Override
        public String toString() {
            StringBuilder rating = new StringBuilder();
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
    private final Function<Sequences<T>, String> prettyPrint;

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases) {
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        slice(candidates, phrases, Objects::toString);
        return candidates.getResult();
    }

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases, List<SlicedPhrases<T>> results,
            Function<Sequences<T>, String> prettyPrint) {
        slice(results, phrases, prettyPrint);
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        results.removeIf(candidate -> candidate.rating.isInvalidated());
        results.stream().forEach(candidates::add);
        return candidates.getResult();
    }

    static <T> void slice(List<SlicedPhrases<T>> candidates, Sequences<T> sequences,
            Function<Sequences<T>, String> prettyPrint) {
        List<SliceInProgress<T>> more = sequences.sliceAll(candidates,
                new SlicedPhrases<>(prettyPrint, sequences.traits.comparator));
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

    public SlicedPhrases(Function<Sequences<T>, String> prettyPrint, Comparator<T> comparator) {
        this.elements = new ArrayList<>();
        this.rating = new Rating<>(comparator);
        this.prettyPrint = prettyPrint;
    }

    private SlicedPhrases(List<Sequences<T>> elements, Rating<T> rating, Sequence.Traits<T> traits,
            Function<Sequences<T>, String> prettyPrint) {
        this.elements = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Sequences<T> sequences = elements.get(i);
            this.elements.add(new Sequences<>(sequences));
        }
        this.rating = new Rating<>(rating, traits.comparator);
        this.prettyPrint = prettyPrint;
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
        return new SlicedPhrases<>(elements, rating, traits, prettyPrint);
    }

    public int maxCommonness() {
        return elements.stream().map(Sequences::max).reduce(Math::max).orElse(0);
    }

    public long distinctSymbolsCount() {
        Traits<T> traits = elements.get(0).traits;
        return elements.stream().flatMap(Sequences::stream).flatMap(Sequence::stream).map(traits.splitter::apply)
                .flatMap(List::stream).map(T::toString).map(String::toLowerCase).distinct().count();
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
            boolean joined = false;
            boolean merged = false;

            Sequences<T> sourceSlice = slice;
            for (int j = elements.size() - 1; j >= 0; j--) {
                Sequences<T> targetSlice = elements.get(j);
                if (targetSlice.isJoinableWith(sequence)) {
                    int sizeBefore = targetSlice.size();

                    sourceSlice.remove(sequence);
                    sourceSlice = targetSlice.joinWith(sequence);
                    elements.set(j, sourceSlice);
                    Sequence<T> _sequence = sequence;
                    // TODO replace startsWith() with equals() - but resolve performance drop
                    Optional<Sequence<T>> findFirst = sourceSlice.stream().filter(moved -> moved.startsWith(_sequence))
                            .findFirst();
                    if (findFirst.isPresent()) {
                        sequence = findFirst.get();
                    } else {
                        throw new NoSuchElementException(sequence.toString());
                    }

                    joined |= sizeBefore + 1 == sourceSlice.size();
                    merged = sizeBefore == sourceSlice.size();
                    rating.updateMaxCommonness(sourceSlice);
                } else {
                    for (Sequence<T> targetSequence : targetSlice) {
                        if (targetSequence.mergeableWith(sequence)) {
                            sourceSlice.remove(sequence);
                            targetSequence.addAll(sequence);
                            targetSlice.remove(targetSequence);
                            targetSlice.add(new Sequence<>(targetSequence, targetSlice.traits));

                            rating.update(sequence);
                            rating.updateMaxCommonness(targetSlice);
                            merged = true;
                            break;
                        }
                    }
                    break;
                }
                if (merged) {
                    break;
                }
            }

            if (joined && !merged) {
                rating.update(sequence);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder phrases = new StringBuilder();
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

        phrases.append(stream().map(prettyPrint).collect(Collectors.joining("\n")));
        return phrases.toString();
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

}
