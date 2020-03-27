package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;
import teaselib.core.speechrecognition.srgs.Sequences.SliceInProgress;

public class SlicedPhrases<T> {
    static class Rating<T> {
        final Set<String> symbols;
        int duplicatedSymbols = 0;
        int maxCommonness = 0;
        
        Rating() {
            this.symbols = new HashSet<>();
        }

        Rating(Rating<T> other) {
            this.symbols = new HashSet<>(other.symbols);
            this.duplicatedSymbols = other.duplicatedSymbols;
            this.maxCommonness = other.maxCommonness;
        }
        
        void update(Sequences<T> slice) {
            slice.stream().forEach(this::update);
            maxCommonness = Math.max(maxCommonness, slice.max());
        }

        public void update(Sequence<T> sequence) {
            sequence.stream() //
                    // TODO duplicated code from duplicatedSymbolsCount
                    .flatMap(e -> sequence.traits.splitter.apply(e).stream()).map(sequence.traits.splitter::apply)
                    .flatMap(List::stream).map(T::toString).map(String::toLowerCase) //
                    .forEach(element -> {
                        if (symbols.contains(element)) {
                            duplicatedSymbols++;
                        } else {
                            symbols.add(element);
                        }
                    });
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
    }

    private final List<Sequences<T>> elements;
    final Rating<T> rating;

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases) {
        Sequences<T> sequences = joinDistinctElements(phrases);
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        slice(candidates, sequences);
        return candidates.getResult();
    }

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases, List<SlicedPhrases<T>> results) {
        Sequences<T> sequences = joinDistinctElements(phrases);
        slice(results, sequences);

        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        results.forEach(candidates::add);
        return candidates.getResult();
    }

    private static <T> Sequences<T> joinDistinctElements(Sequences<T> phrases) {
        SequencesSymbolCount<T> symbols = new SequencesSymbolCount<>(phrases);
        for (Sequence<T> sequence : phrases) {
            for (int i = 0; i < sequence.size(); i++) {
                T t = sequence.get(i);
                if (symbols.get(t) == 1) {
                    joinDistinctElements(sequence, symbols, i, t);
                }
            }
        }
        return phrases;
    }

    private static <T> void joinDistinctElements(Sequence<T> sequence, SequencesSymbolCount<T> symbols, int i, T t) {
        int j = i + 1;
        Sequence<T> distinctSymbols = null;
        while (j < sequence.size()) {
            T u = sequence.get(j);
            if (symbols.get(u) == 1) {
                if (distinctSymbols == null) {
                    distinctSymbols = new Sequence<>(t, sequence.traits);
                }
                sequence.remove(j);
                distinctSymbols.add(u);
            } else {
                break;
            }
        }
        if (distinctSymbols != null) {
            sequence.set(i, distinctSymbols.joined());
        }
    }

    static <T> void slice(List<SlicedPhrases<T>> candidates, Sequences<T> sequences) {
        List<SliceInProgress<T>> more = sequences.sliceAll(candidates, new SlicedPhrases<>());
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

    public SlicedPhrases() {
        this.elements = new ArrayList<>();
        this.rating = new Rating<>();
    }

    public SlicedPhrases(List<Sequences<T>> elements, Rating<T> rating) {
        this.elements = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Sequences<T> sequences = elements.get(i);
            this.elements.add(new Sequences<>(sequences));
        }
        this.rating = new Rating<>(rating);
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

    public SlicedPhrases<T> clone() {
        return new SlicedPhrases<>(elements, rating);
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
            moveDisjunct(slice);
        }

        if (!slice.isEmpty()) {
            rating.update(slice);
            elements.add(slice);
        }
    }

    private void moveDisjunct(Sequences<T> slice) {
        for (Sequence<T> sequence : new ArrayList<>(slice)) {
            boolean joined = false;
            boolean merged = false;

            Sequences<T> sourceSlice = slice;
            for (int j = elements.size() - 1; j >= 0; j--) {
                T joinedSequence = sequence.joined();
                Sequences<T> targetSlice = elements.get(j);
                if (targetSlice.isJoinableWith(joinedSequence)) {
                    int sizeBefore = targetSlice.size();

                    sourceSlice.remove(sequence);
                    sourceSlice = targetSlice.joinWith(sequence);
                    elements.set(j, sourceSlice);
                    sequence = sourceSlice.stream()
                            .filter(moved -> slice.traits.equalsOperator.test(moved.joined(), joinedSequence))
                            .findFirst().orElseThrow();

                    joined |= sizeBefore + 1 == sourceSlice.size();
                    merged = sizeBefore == sourceSlice.size();
                    rating.updateMaxCommonness(sourceSlice);
                } else {
                    for (Sequence<T> targetSequence : targetSlice) {
                        if (targetSequence.mergeableWith(joinedSequence)) {
                            sourceSlice.remove(sequence);
                            targetSequence.add(joinedSequence);
                            targetSlice.remove(targetSequence);
                            targetSlice.add(new Sequence<>(asList(targetSequence.joined()), targetSlice.traits));

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

        phrases.append(stream().map(Object::toString).collect(Collectors.joining("\n")));
        return phrases.toString();
    }

}
