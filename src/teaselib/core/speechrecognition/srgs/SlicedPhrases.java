package teaselib.core.speechrecognition.srgs;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

public class SlicedPhrases<T> {
    static class Rating {
        int duplicatedSymbols = 0;
        int slices;
        int maxCommonness;
    }

    final List<Sequences<T>> elements;
    final Rating rating = new Rating();

    public static <T> SlicedPhrases<T> of(Sequences<T> phrases) {
        Sequences<T> sequences = joinDistinctElements(phrases);
        ReducingList<SlicedPhrases<T>> candidates = new ReducingList<>(SlicedPhrases::leastDuplicatedSymbols);
        candidates.add(slice(candidates, sequences));
        return candidates.element;
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

    static <T> SlicedPhrases<T> slice(List<SlicedPhrases<T>> candidates, Sequences<T> sequences) {
        SlicedPhrases<T> slices = new SlicedPhrases<>();
        return sequences.sliceAll(candidates, slices);
    }

    private static <T> SlicedPhrases<T> leastDuplicatedSymbols(SlicedPhrases<T> a, SlicedPhrases<T> b) {
        long cAa = a.duplicatedSymbolsCount();
        long cAb = b.duplicatedSymbolsCount();
        if (cAa == cAb) {
            int sizeCa = a.size();
            int sizeCb = b.size();
            if (sizeCa == sizeCb) {
                return a.maxCommmonness() > b.maxCommmonness() ? a : b;
            } else {
                return a.size() < b.size() ? a : b;
            }
        } else {
            return cAa < cAb ? a : b;
        }
    }

    public SlicedPhrases() {
        this.elements = new ArrayList<>();
    }

    public SlicedPhrases(int capacity) {
        this.elements = new ArrayList<>(capacity);
    }

    public SlicedPhrases(List<Sequences<T>> elements) {
        super();
        this.elements = elements;
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
        SlicedPhrases<T> clone = new SlicedPhrases<>(size());
        for (int i = 0; i < size(); i++) {
            Sequences<T> sequences = elements.get(i);
            clone.elements.add(new Sequences<>(sequences));
        }
        return clone;
    }

    public int maxCommmonness() {
        return elements.stream().map(Sequences::max).reduce(Math::max).orElse(0);
    }

    public int averageCommonness() {
        return elements.stream().collect(Collectors.summingInt(Sequences::commonness));
    }

    public long symbolCount() {
        return elements.stream().collect(Collectors.summingLong(Sequences::symbolCount));
    }

    public long duplicatedSymbolsCount() {
        long symbols = symbolCount();
        Traits<T> traits = elements.get(0).traits;
        long distinct = elements.stream().flatMap(Sequences::stream).flatMap(Sequence::stream)
                .map(traits.splitter::apply).flatMap(List::stream).map(T::toString).distinct().count();
        return symbols - distinct;
    }

    public void addCompact(Sequences<T> slice) {
        if (!elements.isEmpty()) {
            moveDisjunct(slice);
        }

        if (!slice.isEmpty()) {
            elements.add(slice);
        }
    }

    private void moveDisjunct(Sequences<T> slice) {
        for (Sequence<T> sequence : new ArrayList<>(slice)) {
            Sequences<T> sourceSlice = slice;
            for (int j = elements.size() - 1; j >= 0; j--) {
                T phrase = sequence.joined();
                Sequences<T> targetSlice = elements.get(j);
                if (slice.traits.joinableSequences.test(phrase,
                        targetSlice.stream().flatMap(Sequence::stream).collect(Collectors.toList()))) {
                    sourceSlice.remove(sequence);
                    Sequences<T> joinedTargetSlice = targetSlice.joinWith(sequence);
                    elements.set(j, joinedTargetSlice);
                    targetSlice = joinedTargetSlice;
                    sourceSlice = targetSlice;
                    sequence = sourceSlice.stream()
                            .filter(moved -> slice.traits.equalsOperator.test(moved.joined(), phrase)).findFirst()
                            .orElseThrow();
                } else {
                    for (Sequence<T> targetSequence : new ArrayList<>(targetSlice)) {
                        if (slice.traits.joinablePhrases.test(phrase, targetSequence.joined())) {
                            sourceSlice.remove(sequence);
                            targetSequence.add(phrase);
                            targetSlice.remove(targetSequence);
                            targetSlice.add(new Sequence<>(asList(targetSequence.joined()), targetSlice.traits));
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

}
