package teaselib.core.speechrecognition.srgs;

import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    final Sequence.Traits<T> traits;

    static class SliceInProgress<T> {
        final SlicedPhrases<T> soFar;
        final Sequences<T> unsliced;

        public SliceInProgress(SlicedPhrases<T> soFar, Sequences<T> unsliced) {
            super();
            this.soFar = soFar;
            this.unsliced = unsliced;
        }
    }

    public final List<SliceInProgress<T>> later = new ArrayList<>();

    public Sequences(Traits<T> traits) {
        super();
        this.traits = traits;
    }

    public Sequences(Collection<? extends Sequence<T>> elements, Traits<T> traits) {
        this(traits);
        for (Sequence<T> sequence : elements) {
            Sequence<T> clone = new Sequence<>(traits);
            if (sequence != null) {
                clone.addAll(sequence);
            }
            add(clone);
        }
    }

    public Sequences(int initialCapacity, Traits<T> traits) {
        super(initialCapacity);
        this.traits = traits;
    }

    public Sequences(Sequences<T> other) {
        this(other, other.traits);
    }

    static <T> Sequences<T> of(Iterable<T> elements, Traits<T> traits) {
        Iterator<T> choices = elements.iterator();
        if (!choices.hasNext()) {
            return new Sequences<>(traits);
        } else {
            Sequences<T> sequences = new Sequences<>(traits);
            for (T choice : elements) {
                Sequence<T> e = new Sequence<>(traits.splitter.apply(choice), traits);
                sequences.add(e);
            }
            return sequences;
        }
    }

    public List<SliceInProgress<T>> sliceAll(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> slices) {
        candidates.add(slice(candidates, slices));
        return later;
    }

    private SlicedPhrases<T> slice(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> slices) {
        while (maxLength() > 0) {
            Sequences<T> slice = splitDisjunct(candidates, slices);
            if (slice.isEmpty()) {
                slice = splitCommon(candidates, slices);
            }
            slices.addCompact(slice);
            if (slices.worseThan(candidates)) {
                slices.drop();
                break;
            }
        }
        return slices;
    }

    private Sequences<T> splitDisjunct(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar) {
        SequenceLookupDisjunct<T> lookup = new SequenceLookupDisjunct<>(this);
        lookup.scan();

        int size = size();
        SliceCollector<T> disjunct = new SliceCollector<>(size, traits);
        if (lookup.hasCommonStartElements()) {
            sliceDisjunctVariation(candidates, soFar, disjunct.sequences);
        }

        SliceCollector<T> disjunctWithLaterOccurences = new SliceCollector<>(size, traits);
        SliceCollector<T> disjunctWithoutLaterOccurences = new SliceCollector<>(size, traits);

        for (int i = 0; i < size; i++) {
            Sequence<T> sequence = get(i);
            if (!sequence.isEmpty()) {
                var element = sequence.get(0);
                boolean othersStartWithElement = lookup.othersStartWith(element);
                if (!othersStartWithElement) {
                    disjunct.add(i, element);
                    if (lookup.occursLaterInAnotherSequence(sequence, element)) {
                        disjunctWithLaterOccurences.add(i, element);
                    } else {
                        disjunctWithoutLaterOccurences.add(i, element);
                    }
                }
            }
        }

        Map<Integer, Set<T>> distances = new SymbolDistances<>(this).groups();
        for (Entry<Integer, Set<T>> distance : distances.entrySet()) {
            Set<T> symbols = distance.getValue();
            if (symbols.size() == size) {
                // same distance for all symbols -> slice with all but one to end recursion
                for (T element : symbols) {
                    TreeSet<T> symbolsWithoutElement = new TreeSet<>(traits.comparator);
                    symbolsWithoutElement.addAll(symbols);
                    symbolsWithoutElement.remove(element);
                    sliceWithLaterOccurrences(candidates, soFar, disjunct, symbolsWithoutElement);
                }
            } else {
                sliceWithLaterOccurrences(candidates, soFar, disjunct, symbols);
            }
        }

        remove(disjunct.sequences.sizes());
        return disjunct.slice();
    }

    private void sliceWithLaterOccurrences(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            SliceCollector<T> disjunct, Set<T> symbolsWithLaterOccurence) {
        SliceCollector<T> disjunctWithoutLaterOccurrencesAtDistance = disjunct.without(symbolsWithLaterOccurence);

        if (disjunctWithoutLaterOccurrencesAtDistance.modified) {
            disjunctWithoutLaterOccurrencesAtDistance.modified = false;
            if (!disjunctWithoutLaterOccurrencesAtDistance.equals(disjunct)) {
                sliceDisjunctVariation(candidates, soFar, disjunctWithoutLaterOccurrencesAtDistance.sequences);
            }
        }
    }

    private void sliceDisjunctVariation(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            Sequences<T> disjunctElements) {

        SlicedPhrases<T> soFarClone = soFar.clone(traits);
        soFarClone.addCompact(createDisjunctSlice(disjunctElements));

        Sequences<T> unsliced = new Sequences<>(this);
        unsliced.remove(disjunctElements.sizes());
        soFarClone.addCompact(unsliced.splitCommon(candidates, soFarClone));

        SlicedPhrases<T> candidate = unsliced.slice(candidates, soFarClone);
        candidates.add(candidate);
    }

    List<Integer> sizes() {
        return stream().map(sequence -> sequence != null && !sequence.isEmpty() ? sequence.size() : 0)
                .collect(toList());
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || stream().filter(Objects::nonNull).allMatch(Sequence::isEmpty);
    }

    private Sequences<T> createDisjunctSlice(Sequences<T> disjunct) {
        List<Sequence<T>> elements = disjunct.stream().filter(Objects::nonNull).filter(Sequence::nonEmpty)
                .map(element -> new Sequence<>(element, traits)).collect(toList());
        return new Sequences<>(elements, traits);
    }

    public long symbolCount() {
        return stream().flatMap(Sequence::stream).count();
    }

    public int maxCommonness() {
        int max = Integer.MIN_VALUE;
        for (Sequence<T> sequence : this) {
            max = Math.max(max, sequence.maxCommonness());
        }
        return max;
    }

    Sequence<T> get(int index, Supplier<Sequence<T>> supplier) {
        Sequence<T> sequence = get(index);
        if (sequence == null) {
            sequence = supplier.get();
            set(index, sequence);
        }
        return sequence;
    }

    @SuppressWarnings("rawtypes")
    private static final Sequences empty = new Sequences<>((Sequence.Traits<?>) null);

    private Sequences<T> splitCommon(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar) {
        Optional<Integer> maxCommon = maxCommon(0, 1);
        if (maxCommon.isPresent()) {
            SliceCollector<T> common = new SliceCollector<>(size(), traits, () -> new Sequence<>(traits));
            SequenceLookupCommon<T> lookup = new SequenceLookupCommon<>(this, traits.listComparator);
            // compute the maximum wide common slice for elements and successors up to "length"
            int length = 1;
            while (true) {
                lookup.scan(length);
                boolean success = setCommonToStartElements(common, length, lookup);
                if (!success) {
                    break;
                } else {
                    SliceCollector<T> shorter = computeShorterCommmon(common);
                    if (shorter != null && !shorter.isEmpty) {
                        SlicedPhrases<T> soFarClone = soFar.clone(traits);
                        soFarClone.addCompact(shorter.gather());

                        var withoutElement = new Sequences<>(this);
                        withoutElement.removeCommon(shorter.sequences);
                        SlicedPhrases<T> candidate = withoutElement.slice(candidates, soFarClone);
                        candidates.add(candidate);
                    }

                    boolean canExit = inspectElementsThatOccurLater(candidates, soFar, common, maxCommon.get(), lookup,
                            length);
                    if (canExit) {
                        break;
                    } else {
                        length++;
                    }
                }
            }
            Sequences<T> commonSlice = common.gather();
            removeCommon(common.sequences);
            return commonSlice;
        } else {
            return empty;
        }
    }

    private boolean setCommonToStartElements(SliceCollector<T> common, int length, SequenceLookupCommon<T> lookup) {
        boolean success = false;
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            if (sequence.size() >= length) {
                List<T> startElements = sequence.subList(0, length);
                if (lookup.othersStartWith(startElements)) {
                    common.set(i, new Sequence<>(startElements, traits));
                    success = true;
                }
            }
        }
        return success;
    }

    private SliceCollector<T> computeShorterCommmon(SliceCollector<T> common) {
        Sequences<T> commonSlice = common.gather();

        Set<Sequence<T>> remove = null;
        int size = common.size();
        for (int i = 0; i < size; i++) {
            Sequence<T> sequence = common.get(i);
            for (Sequence<T> sequence2 : commonSlice) {
                if (get(i).matchesAt(sequence2, sequence.size())) {
                    if (remove == null) {
                        remove = new HashSet<>();
                    }
                    remove.add(sequence2);
                    break;
                }
            }
        }

        if (remove == null) {
            return null;
        } else {
            var shorter = new SliceCollector<>(size, traits);
            for (int i = 0; i < size; i++) {
                Sequence<T> sequence = common.get(i);
                if (remove.stream().anyMatch(e -> e.startsWith(sequence))) {
                    shorter.set(i, new Sequence<>(traits));
                } else {
                    shorter.set(i, sequence);
                }
            }
            return shorter;
        }
    }

    private boolean inspectElementsThatOccurLater(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            SliceCollector<T> common, Integer maxCommon, SequenceLookupCommon<T> lookup, int expectedLength) {
        boolean canExit = false;
        Optional<Integer> nextMaxCommon = maxCommonAfter(common);
        if (nextMaxCommon.isEmpty() || nextMaxCommon.get().equals(maxCommon)) {
            Sequences<T> commonSlice = common.gather();
            for (int i = commonSlice.size() - 1; i >= 0; i--) {
                Sequence<T> commonPhrase = commonSlice.get(i);
                if (commonPhrase.size() == expectedLength && lookup.occursLaterInAnotherSequence(commonPhrase)) {
                    SlicedPhrases<T> candidate = sliceCommonWithoutStartElements(candidates, soFar, commonPhrase);
                    candidates.add(candidate);
                }
            }
        } else {
            canExit = true;
        }
        return canExit;
    }

    private SlicedPhrases<T> sliceCommonWithoutStartElements(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            Sequence<T> startElements) {
        SlicedPhrases<T> candidate = soFar.clone(traits);

        candidate.addCompact(new Sequences<>(singleton(startElements), traits));

        Sequences<T> withoutElement = new Sequences<>(this);
        for (Sequence<T> sequence : withoutElement) {
            if (sequence.startsWith(startElements)) {
                sequence.remove(0, startElements.size());
            }
        }

        return withoutElement.slice(candidates, candidate);
    }

    private Optional<Integer> maxCommonAfter(SliceCollector<T> sliceCollector) {
        List<Integer> startIndices = sliceCollector.stream()
                .map(sequence -> sequence != null && !sequence.isEmpty() ? sequence.size() : 0).collect(toList());
        Sequences<T> remaining = remaining(startIndices);
        return remaining.maxCommon(0, 1);
    }

    private Sequences<T> remaining(List<Integer> from) {
        var remaining = new Sequences<>(size(), traits);

        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            remaining.add(new Sequence<>(sequence.subList(from.get(i), sequence.size()), traits));
        }
        return remaining;
    }

    private void remove(List<Integer> sizes) {
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            sequence.remove(0, sizes.get(i));
        }
    }

    private Optional<Integer> maxCommon(int start, int length) {
        Map<List<T>, AtomicInteger> distinct = new TreeMap<>(traits.listComparator);

        for (Sequence<T> sequence : this) {
            int size = sequence.size();
            if (size > start) {
                List<T> key = sequence.subList(start, min(start + length, size));
                distinct.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
            }
        }

        if (distinct.isEmpty()) {
            return Optional.empty();
        }

        int max = Integer.MIN_VALUE;
        for (AtomicInteger value : distinct.values()) {
            int v = value.get();
            if (v > max)
                max = v;
        }
        return Optional.of(max);
    }

    private void removeCommon(List<Sequence<T>> common) {
        for (int i = 0; i < common.size(); i++) {
            Sequence<T> commonSlice = common.get(i);
            if (!commonSlice.isEmpty()) {
                Sequence<T> sequence = get(i);
                if (!sequence.isEmpty()) {
                    sequence.remove(0, commonSlice.size());
                }
            }
        }
    }

    public int maxLength() {
        Optional<? extends Sequence<T>> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    public boolean isJoinableWith(Sequence<T> sequence) {
        return traits.joinableSequences.test(sequence, stream().flatMap(Sequence::stream).collect(toList()));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (!super.equals(obj))
            return false;
        else if (this.getClass().isAssignableFrom(obj.getClass()) || obj.getClass().isAssignableFrom(getClass()))
            return true;
        return false;
    }

}
