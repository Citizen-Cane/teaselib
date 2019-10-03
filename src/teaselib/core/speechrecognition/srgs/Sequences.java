package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import teaselib.core.speechrecognition.srgs.Sequence.Traits;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    final Sequence.Traits<T> traits;

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

    public static <T> List<Sequences<T>> of(Iterable<T> elements, Traits<T> traits) {
        Iterator<T> choices = elements.iterator();
        if (!choices.hasNext()) {
            return Collections.emptyList();
        } else {
            Sequences<T> sequences = new Sequences<>(traits);
            for (T choice : elements) {
                Sequence<T> e = new Sequence<>(traits.splitter.apply(choice), traits);
                sequences.add(e);
            }
            return slice(sequences);
        }
    }

    private static <T> List<Sequences<T>> slice(Sequences<T> sequences) {
        List<List<Sequences<T>>> candidates = new ArrayList<>();

        List<Sequences<T>> candidate = slice(candidates, sequences);
        candidates.add(candidate);

        return Sequences.reduce(candidates);
    }

    private static <T> List<Sequences<T>> slice(List<List<Sequences<T>>> candidates, Sequences<T> sequences) {
        List<Sequences<T>> slices = new ArrayList<>();
        return slice(candidates, slices, sequences);
    }

    private static <T> List<Sequences<T>> slice(List<List<Sequences<T>>> candidates, List<Sequences<T>> slices,
            Sequences<T> sequences) {
        while (sequences.maxLength() > 0) {
            slices.add(sequences.slice(candidates, slices));
        }
        return slices;
    }

    /**
     * Determine the first slice, remove it from the sequences and return it
     * 
     * @return The first slice - n sequences each with 1-n choice indices -> covering all choice indices
     * 
     *         Each slice contains either:
     *         <li>the maximum independent elements (size = choices.size)
     *         <li>the maximum reduced elements with minimum size (size < choices.size) How to start:
     *         <li>Find any two common parts
     *         <li>if !disjunct.empty return disjunct
     *         <li>return maximum reduced part with minimized size
     * 
     */
    Sequences<T> slice(List<List<Sequences<T>>> candidates, List<Sequences<T>> soFar) {
        Sequences<T> slice = splitDisjunct(candidates, soFar);
        if (slice.isEmpty()) {
            slice = splitCommon(candidates, soFar);
        }

        return slice;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || stream().allMatch(Sequence::isEmpty);
    }

    private void splitDisjunctUniqueElements(Sequences<T> disjunct) {
        SequenceLookup<T> distinct2 = new SequenceLookup<>(this);
        distinct2.scan(1);
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            if (!sequence.isEmpty()) {
                T element = sequence.get(0);
                while (!distinct2.othersStartWith(element) && !distinct2.occursInAnotherDistinctSequence(element)) {
                    disjunct.get(i, () -> new Sequence<>(traits)).add(element);
                    // Invalidates distinct lookup
                    sequence.remove(element);
                    distinct2.scan(1);
                    if (sequence.isEmpty()) {
                        break;
                    } else {
                        element = sequence.get(0);
                    }
                }
            }
        }
    }

    private Sequences<T> splitDisjunct(List<List<Sequences<T>>> candidates, List<Sequences<T>> soFar) {
        Sequences<T> disjunct = new Sequences<>(size(), traits);
        for (int i = 0; i < size(); i++) {
            disjunct.add(null);
        }

        splitDisjunctUniqueElements(disjunct);

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
                            List<Sequences<T>> candidate = clone(soFar);

                            Sequences<T> current = new Sequences<>(disjunct);
                            current.get(i, () -> new Sequence<>(traits)).add(element);
                            current = new Sequences<>(current.stream().filter(Sequence::nonEmpty).collect(toList()),
                                    traits);
                            candidate.add(current);

                            Sequences<T> withoutElement = new Sequences<>(this);
                            withoutElement.get(i).remove(element);
                            List<Sequences<T>> slices = slice(candidates, candidate, withoutElement);
                            candidates.add(slices);
                        } else {
                            disjunct.get(i, () -> new Sequence<>(traits)).add(element);
                            sequence.remove(element);
                            distinct.scan(1);
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

    private static <T> List<Sequences<T>> clone(List<Sequences<T>> multipleSequences) {
        List<Sequences<T>> clone = new ArrayList<>(multipleSequences.size());
        for (int i = 0; i < multipleSequences.size(); i++) {
            Sequences<T> sequences = multipleSequences.get(i);
            clone.add(new Sequences<>(sequences));
        }
        return clone;
    }

    private Sequences<T> createDisjunctSlice(Sequences<T> disjunct) {
        List<Sequence<T>> elements = disjunct.stream().filter(Objects::nonNull).filter(Sequence::nonEmpty)
                .map(traits.joinSequenceOperator::apply).map(element -> new Sequence<>(element, traits))
                .collect(toList());
        return new Sequences<>(elements, traits);
    }

    public static <T> List<Sequences<T>> reduce(List<List<Sequences<T>>> candidates) {
        return candidates.stream().reduce((a, b) -> {
            int ca = commonness(a);
            int cb = commonness(b);
            if (ca == cb) {
                return max(a) > max(b) ? a : b;
            } else {
                return ca > cb ? a : b;
            }

        }).orElseThrow();
    }

    public int commonness() {
        return stream().flatMap(Sequence::stream).map(traits.commonnessOperator::applyAsInt).reduce(0,
                (x, y) -> x + y - 1);
    }

    public static <T> int max(List<Sequences<T>> sequences) {
        return sequences.stream().flatMap(Sequences::stream).flatMap(Sequence::stream)
                .map(t -> (((PhraseString) t).indices.size())).reduce(Math::max).orElse(0);
    }

    public static <T> int commonness(List<Sequences<T>> sequences) {
        return sequences.stream().map(Sequences::commonness).reduce(0, (x, y) -> x + y);
    }

    private Sequence<T> get(int index, Supplier<Sequence<T>> supplier) {
        Sequence<T> sequence = get(index);
        if (sequence == null) {
            sequence = supplier.get();
            set(index, sequence);
        }
        return sequence;
    }

    private Sequences<T> splitCommon(List<List<Sequences<T>>> candidates, List<Sequences<T>> soFar) {
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
                boolean nothingFound1 = true;
                for (int i1 = 0; i1 < size(); i1++) {
                    Sequence<T> sequence = get(i1);
                    if (sequence.size() >= length) {
                        List<T> startElements1 = sequence.subList(0, length);
                        if (distinct.othersStartWith(startElements1)) {
                            common.set(i1, new Sequence<>(startElements1, traits));
                            nothingFound1 = false;
                        }
                    }
                }

                boolean nothingFound = nothingFound1;
                if (nothingFound) {
                    break;
                } else {
                    Optional<Integer> nextMaxCommon = maxCommonAfter(common);
                    if (nextMaxCommon.isEmpty() || nextMaxCommon.get().equals(maxCommon.get())) {
                        Sequences<T> slice = new Sequences<>(this).gatherCommonElements(common);
                        for (int i = 0; i < slice.size(); i++) {
                            Sequence<T> startElements = slice.get(i);
                            if (distinct.occursLaterInAnotherSequence(startElements)) {
                                List<Sequences<T>> candidate = sliceCommonWithoutStartElements(candidates, soFar,
                                        startElements);
                                candidates.add(candidate);
                            }
                        }
                        length++;
                    } else {
                        break;
                    }
                }
            }
        }

        return gatherCommonSlice(common);
    }

    private List<Sequences<T>> sliceCommonWithoutStartElements(List<List<Sequences<T>>> candidates,
            List<Sequences<T>> soFar, Sequence<T> startElements) {
        List<Sequences<T>> candidate = clone(soFar);
        candidate.add(new Sequences<>(Collections.singleton(startElements), traits));
        Sequences<T> withoutElement = new Sequences<>(this);
        for (Sequence<T> sequence : withoutElement) {
            if (sequence.startsWith(startElements)) {
                sequence.remove(0, startElements.size());
            }
        }

        return slice(candidates, candidate, withoutElement);
    }

    private Sequences<T> gatherCommonElements(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> distinct = distinct(candidates);
        Sequences<T> common = new Sequences<>(traits);
        distinct.values().stream().map(Sequence<T>::new).forEach(common::add);
        return common;
    }

    private Sequences<T> gatherCommonSlice(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> distinct = distinct(candidates);
        Sequences<T> common = new Sequences<>(traits);
        distinct.values().stream().map(traits.joinSequenceOperator::apply)
                .map(element -> new Sequence<>(element, traits)).forEach(common::add);
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
        stream().filter(seq -> seq.size() > start).map(seq -> seq.subList(start, Math.min(start + length, seq.size())))
                .map(Objects::toString).map(String::toLowerCase)
                .forEach(s -> distinct.computeIfAbsent(s, t -> new AtomicInteger(0)).incrementAndGet());
        return distinct.values().stream().map(AtomicInteger::intValue).reduce(Math::max);
    }

    private Map<String, Sequence<T>> distinct(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> reduced = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            Sequence<T> elements = candidates.get(i);
            if (!elements.isEmpty()) {
                Sequence<T> candidate = new Sequence<>(elements, traits);
                if (!candidate.isEmpty()) {
                    // TODO 1.b resolve conflict with equalsOperator result, use type T instead of String
                    String key = traits.joinSequenceOperator.apply(candidate).toString().toLowerCase();
                    if (reduced.containsKey(key)) {
                        Sequence<T> existing = reduced.get(key);
                        Sequence<T> joined = new Sequence<>(existing, traits);
                        joined.addAll(candidate);
                        Sequence<T> joinedElements = new Sequence<>(traits);
                        for (int j = 0; j < existing.size(); j++) {
                            joinedElements.add(
                                    traits.joinCommonOperator.apply(Arrays.asList(candidate.get(j), existing.get(j))));
                        }
                        reduced.put(key, joinedElements);
                    } else {
                        reduced.put(key, candidate);
                    }

                    // TODO Generalize and use op
                    int l = candidate.toString().split(" ").length;
                    get(i).remove(0, l);
                }
            }
        }
        return reduced;
    }

    Sequence<T> commonStart() {
        Sequence<T> sequence = new Sequence<>(get(0), traits);
        for (int i = sequence.size(); i >= 1; --i) {
            if (allStartWithSequence(sequence)) {
                return common(sequence);
            }
            sequence.remove(sequence.size() - 1);
        }

        return new Sequence<>(Collections.emptyList(), traits);
    }

    private boolean allStartWithSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).startsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    Sequence<T> commonEnd() {
        Sequence<T> sequence = new Sequence<>(get(0), traits);
        for (int i = sequence.size(); i >= 1; --i) {
            if (allEndWithSequence(sequence)) {
                return common(sequence);
            }
            sequence.remove(0);
        }

        return new Sequence<>(Collections.emptyList(), traits);
    }

    private boolean allEndWithSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (!get(j).endsWith(sequence)) {
                return false;
            }
        }
        return true;
    }

    Sequence<T> commonMiddle() {
        Sequence<T> sequence = new Sequence<>(get(0), traits);

        int lastElement = sequence.size() - 1;
        if (lastElement > 0 && sequence.get(lastElement).toString().isBlank()) {
            sequence.remove(lastElement);
        }

        List<Sequence<T>> candidates = sequence.subLists();
        for (Sequence<T> candidate : candidates) {
            if (allContainMiddleSequence(candidate)) {
                return common(candidate);
            }
        }

        return new Sequence<>(Collections.emptyList(), traits);
    }

    private Sequence<T> common(Sequence<T> candidate) {
        List<Sequence<T>> common = stream().map(s -> s.subList(candidate)).collect(toList());
        List<T> joined = new ArrayList<>(candidate.size());
        for (int i = 0; i < candidate.size(); i++) {
            final int index = i;
            List<T> slice = common.stream().map(e -> e.get(index)).collect(toList());
            joined.add(traits.joinCommonOperator.apply(slice));
        }
        return new Sequence<>(joined, traits);
    }

    private boolean allContainMiddleSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (get(j).indexOf(sequence) == -1) {
                return false;
            }
        }
        return true;
    }

    public boolean containsOptionalParts() {
        return stream().map(Sequence<T>::toString).anyMatch(String::isEmpty);
    }

    static <T> int phraseCount(List<Sequences<T>> phrases) {
        return phrases.stream().map(List::size).reduce(Math::max).orElse(0);
    }

    public int maxLength() {
        Optional<? extends Sequence<T>> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    public List<String> toStrings() {
        return stream().map(Sequence::toString).collect(Collectors.toList());
    }

    public static <T> List<T> flatten(List<Sequences<T>> sliced, Sequence.Traits<T> traits) {
        int phraseCount = phraseCount(sliced);
        if (phraseCount == 0) {
            return Collections.emptyList();
        }

        List<T> flattened = new ArrayList<>(phraseCount);

        for (int phraseIndex = 0; phraseIndex < phraseCount; phraseIndex++) {
            Sequence<T> phrase = new Sequence<>(traits);

            for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
                Sequences<T> sequences = sliced.get(ruleIndex);
                Sequence<T> sequence = sequences.get(Math.min(phraseIndex, sequences.size() - 1));
                phrase.addAll(sequence);
            }

            flattened.add(traits.joinSequenceOperator.apply(phrase));
        }

        return flattened;
    }

    public Sequences<T> joinWith(Sequences<T> second) {
        Sequences<T> joined = new Sequences<>(traits);
        if (this.size() > second.size()) {
            for (int i = 0; i < this.size(); i++) {
                List<T> elements = new ArrayList<>(get(i));
                elements.addAll(second.get(0));
                joined.add(new Sequence<>(traits.joinSequenceOperator.apply(elements), traits));
            }
        } else if (this.size() < second.size()) {
            for (int i = 0; i < second.size(); i++) {
                List<T> elements = new ArrayList<>(get(0));
                elements.addAll(second.get(i));
                joined.add(new Sequence<>(traits.joinSequenceOperator.apply(elements), traits));
            }
        } else {
            for (int i = 0; i < second.size(); i++) {
                List<T> elements = new ArrayList<>(get(i));
                elements.addAll(second.get(i));
                joined.add(new Sequence<>(traits.joinSequenceOperator.apply(elements), traits));
            }
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
        if (!super.equals(obj))
            return false;
        if (this.getClass().isAssignableFrom(obj.getClass()) || obj.getClass().isAssignableFrom(getClass()))
            return true;
        return false;
    }

}
