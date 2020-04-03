package teaselib.core.speechrecognition.srgs;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        super(elements);
        this.traits = traits;
    }

    public Sequences(int initialCapacity, Traits<T> traits) {
        super(initialCapacity);
        this.traits = traits;
    }

    public Sequences(Sequences<T> other) {
        this(other.traits);
        for (Sequence<T> sequence : other) {
            Sequence<T> clone = new Sequence<>(traits);
            if (sequence != null) {
                clone.addAll(sequence);
            }
            add(clone);
        }
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
            Sequences<T> slice = splitDisjunct(slices);
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

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || stream().allMatch(Sequence::isEmpty);
    }

    private Sequences<T> removeDisjunctElements() {
        Sequences<T> disjunct = new Sequences<>(size(), traits);
        for (int i = 0; i < size(); i++) {
            disjunct.add(null);
        }

        SequenceLookup<T> distinct = new SequenceLookup<>(this);
        distinct.scan(1);
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            if (!sequence.isEmpty()) {
                T element = sequence.get(0);
                while (!distinct.othersStartWith(element) && !distinct.occursInAnotherDistinctSequence(element)) {
                    disjunct.get(i, () -> new Sequence<>(traits)).add(element);
                    distinct.removeAndRescan(sequence, element, 1);
                    if (sequence.isEmpty()) {
                        break;
                    } else {
                        element = sequence.get(0);
                    }
                }
            }
        }

        return disjunct;
    }

    private Sequences<T> splitDisjunct(SlicedPhrases<T> soFar) {
        Sequences<T> disjunct = removeDisjunctElements();
        int length = 1;
        while (!isEmpty()) {
            SequenceLookup<T> distinct = new SequenceLookup<>(this);
            distinct.scan(length);
            boolean elementRemoved = false;
            for (int i = 0; i < size(); i++) {
                Sequence<T> sequence = get(i);
                if (!sequence.isEmpty()) {
                    T element = sequence.get(0);
                    if (!distinct.othersStartWith(element)) {
                        if (distinct.occursInAnotherDistinctSequence(element) && distinct.hasCommonStartElements()) {
                            SlicedPhrases<T> candidate = soFar.clone();

                            Sequences<T> current = new Sequences<>(disjunct);
                            current.get(i, () -> new Sequence<>(traits)).add(element);
                            current = new Sequences<>(
                                    current.stream().filter(Sequence::nonEmpty).map(traits.joinSequenceOperator::apply)
                                            .map(joined -> new Sequence<>(joined, traits)).collect(toList()),
                                    traits);
                            candidate.addCompact(current);

                            Sequences<T> withoutElement = new Sequences<>(this);
                            withoutElement.get(i).remove(element);

                            later.add(new SliceInProgress<>(candidate, withoutElement));
                        } else {
                            disjunct.get(i, () -> new Sequence<>(traits)).add(element);
                            distinct.removeAndRescan(sequence, element, 1);
                            elementRemoved = true;
                        }
                    }
                }
            }
            if (!elementRemoved) {
                break;
            }
        }

        return createDisjunctSlice(disjunct);
    }

    private Sequences<T> createDisjunctSlice(Sequences<T> disjunct) {
        List<Sequence<T>> elements = disjunct.stream().filter(Objects::nonNull).filter(Sequence::nonEmpty)
                .map(traits.joinSequenceOperator::apply).map(element -> new Sequence<>(element, traits))
                .collect(toList());
        return new Sequences<>(elements, traits);
    }

    public long symbolCount() {
        return stream().flatMap(Sequence::stream).map(traits.splitter::apply).flatMap(List::stream).count();
    }

    public int max() {
        return stream().flatMap(Sequence::stream).map(traits.commonnessOperator::applyAsInt).reduce(Math::max)
                .orElse(0);
    }

    private Sequence<T> get(int index, Supplier<Sequence<T>> supplier) {
        Sequence<T> sequence = get(index);
        if (sequence == null) {
            sequence = supplier.get();
            set(index, sequence);
        }
        return sequence;
    }

    private Sequences<T> splitCommon(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar) {
        Sequences<T> common = new Sequences<>(size(), traits);
        for (int n = 0; n < size(); n++) {
            common.add(new Sequence<>(traits));
        }

        Optional<Integer> maxCommon = maxCommon(0, 1);
        if (maxCommon.isPresent()) {
            int length = 1;
            while (true) {
                SequenceLookup<T> distinct = new SequenceLookup<>(this);
                distinct.scan(length);
                boolean success = setCommonToStartElements(common, length, distinct);
                if (!success) {
                    break;
                } else {
                    Sequences<T> shorter = computeShorterCommmon(common);
                    if (shorter.equals(common)) {
                        // Nothing to do
                    } else {
                        SlicedPhrases<T> soFarClone = soFar.clone();
                        soFarClone.addCompact(new Sequences<>(gatherCommonElements(shorter), traits));

                        Sequences<T> withoutElement = new Sequences<>(this);
                        withoutElement.removeCommon(shorter);
                        SlicedPhrases<T> candidate = withoutElement.slice(candidates, soFarClone);
                        candidates.add(candidate);
                    }

                    boolean canExit = inspectElementsThatOccurLater(candidates, soFar, common, maxCommon.get(),
                            distinct);
                    if (canExit) {
                        break;
                    } else {
                        length++;
                    }
                }
            }
        }

        Sequences<T> commonSlice = gatherCommonElements(common);
        removeCommon(common);
        return commonSlice;
    }

    private boolean setCommonToStartElements(Sequences<T> common, int length, SequenceLookup<T> distinct) {
        boolean success = false;
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            if (sequence.size() >= length) {
                List<T> startElements = sequence.subList(0, length);
                if (distinct.othersStartWith(startElements)) {
                    common.set(i, new Sequence<>(startElements, traits));
                    success = true;
                }
            }
        }
        return success;
    }

    private boolean inspectElementsThatOccurLater(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            Sequences<T> common, Integer maxCommon, SequenceLookup<T> distinct) {
        boolean canExit = false;
        Optional<Integer> nextMaxCommon = maxCommonAfter(common);
        if (nextMaxCommon.isEmpty() || nextMaxCommon.get().equals(maxCommon)) {
            Sequences<T> slice = gatherCommonElements(common);
            for (int i = slice.size() - 1; i >= 0; i--) {
                Sequence<T> startElements = slice.get(i);
                if (distinct.occursLaterInAnotherSequence(startElements)) {
                    SlicedPhrases<T> candidate = sliceCommonWithoutStartElements(candidates, soFar, startElements);
                    candidates.add(candidate);
                    break;
                }
            }
        } else {
            canExit = true;
        }
        return canExit;
    }

    private Sequences<T> computeShorterCommmon(Sequences<T> common) {
        Sequences<T> commonElements = new Sequences<>(this).gatherCommonElements(common);
        Sequences<T> shorter = new Sequences<>(traits);
        Set<Sequence<T>> remove = new HashSet<>();
        for (int i = 0; i < common.size(); i++) {
            Sequence<T> sequence = common.get(i);
            for (Sequence<T> sequence2 : commonElements) {
                if (new Sequence<>(this.get(i).subList(sequence.size()), traits).startsWith(sequence2)) {
                    remove.add(sequence2);
                    break;
                }
            }
            shorter.add(sequence);
        }
        for (int i = 0; i < shorter.size(); i++) {
            Sequence<T> sequence = shorter.get(i);
            if (remove.stream().anyMatch(e -> e.startsWith(sequence))) {
                shorter.set(i, new Sequence<>(traits));
            }
        }
        return shorter;
    }

    private SlicedPhrases<T> sliceCommonWithoutStartElements(List<SlicedPhrases<T>> candidates, SlicedPhrases<T> soFar,
            Sequence<T> startElements) {
        SlicedPhrases<T> candidate = soFar.clone();

        candidate.addCompact(new Sequences<>(singleton(startElements), traits));

        Sequences<T> withoutElement = new Sequences<>(this);
        for (Sequence<T> sequence : withoutElement) {
            if (sequence.startsWith(startElements)) {
                sequence.remove(0, startElements.size());
            }
        }

        return withoutElement.slice(candidates, candidate);
    }

    private Sequences<T> gatherCommonElements(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> distinct = distinct(candidates);
        Sequences<T> common = new Sequences<>(traits);
        distinct.values().stream().map(Sequence<T>::new).forEach(common::add);
        return common;
    }

    private Optional<Integer> maxCommonAfter(List<Sequence<T>> sequences) {
        List<Integer> startIndices = sequences.stream()
                .map(sequence -> sequence != null && !sequence.isEmpty() ? sequence.size() : 0).collect(toList());
        Sequences<T> remaining = remaining(startIndices);
        return remaining.maxCommon(0, 1);
    }

    private Sequences<T> remaining(List<Integer> from) {
        Sequences<T> remaining = new Sequences<>(size(), traits);

        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            remaining.add(new Sequence<>(sequence.subList(from.get(i), sequence.size()), traits));
        }
        return remaining;
    }

    private Optional<Integer> maxCommon(int start, int length) {
        Map<String, AtomicInteger> distinct = new HashMap<>(size());
        // TODO use generic equalsOperator to accumulate occurrence into distinct map
        stream().filter(seq -> seq.size() > start).map(seq -> seq.subList(start, min(start + length, seq.size())))
                .map(Objects::toString).map(String::toLowerCase)
                .forEach(s -> distinct.computeIfAbsent(s, t -> new AtomicInteger(0)).incrementAndGet());
        return distinct.values().stream().map(AtomicInteger::intValue).reduce(Math::max);
    }

    private Map<String, Sequence<T>> distinct(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> reduced = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            Sequence<T> elements = candidates.get(i);
            if (!elements.isEmpty()) {
                String key = elements.joined().toString().toLowerCase();
                if (reduced.containsKey(key)) {
                    Sequence<T> joinedElements = new Sequence<>(traits);
                    Sequence<T> existing = reduced.get(key);
                    for (int j = 0; j < existing.size(); j++) {
                        joinedElements.add(traits.joinCommonOperator.apply(asList(elements.get(j), existing.get(j))));
                    }
                    reduced.put(key, joinedElements);
                } else {
                    reduced.put(key, elements);
                }
            }
        }
        return reduced;
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
        return traits.joinableSequences.test(sequence, stream().flatMap(Sequence::stream).collect(Collectors.toList()));
    }

    public Sequences<T> joinWith(Sequence<T> sequence) {
        boolean merged = false;
        Sequences<T> joined = new Sequences<>(traits);
        for (Sequence<T> element : this) {
            if (!merged && sequence.equals(element)) {
                Sequence<T> joinedSequence = new Sequence<>(traits);
                for (int i = 0; i < sequence.size(); i++) {
                    joinedSequence.add(traits.joinCommonOperator.apply(Arrays.asList(sequence.get(i), element.get(i))));
                }
                joined.add(joinedSequence);
                merged = true;
            } else {
                joined.add(element);
            }
        }

        if (!merged) {
            joined.add(sequence);
        }

        return joined;
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
