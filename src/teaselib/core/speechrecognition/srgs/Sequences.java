package teaselib.core.speechrecognition.srgs;

import static java.util.stream.Collectors.*;

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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    final transient BiPredicate<T, T> equalsOperator;
    final transient Function<List<T>, T> joinCommonOperator;
    final transient Function<List<T>, T> joinSequenceOperator;

    public Sequences(BiPredicate<T, T> equalsOperator, Function<List<T>, T> joinOperator,
            Function<List<T>, T> joinSequenceOperator) {
        super();
        this.equalsOperator = equalsOperator;
        this.joinCommonOperator = joinOperator;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public Sequences(Collection<? extends Sequence<T>> elements, BiPredicate<T, T> equalsOperator,
            Function<List<T>, T> joinCommonOperator, Function<List<T>, T> joinSequenceOperator) {
        super(elements);
        this.equalsOperator = equalsOperator;
        this.joinCommonOperator = joinCommonOperator;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public Sequences(int initialCapacity, BiPredicate<T, T> equalsOperator, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator) {
        super(initialCapacity);
        this.equalsOperator = equalsOperator;
        this.joinCommonOperator = joinCommonOperator;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public Sequences(Sequences<T> other) {
        this(other.equalsOperator, other.joinCommonOperator, other.joinSequenceOperator);
        for (Sequence<T> sequence : other) {
            Sequence<T> clone = new Sequence<>(equalsOperator);
            if (sequence != null) {
                clone.addAll(sequence);
            }
            add(clone);
        }
    }

    public static <T> List<Sequences<T>> of(Iterable<T> elements, BiPredicate<T, T> equalsOperator,
            Function<T, List<T>> splitter, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator) {
        Iterator<T> choices = elements.iterator();
        if (!choices.hasNext()) {
            return Collections.emptyList();
        } else {
            Sequences<T> sequences = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
            for (T choice : elements) {
                Sequence<T> e = new Sequence<>(splitter.apply(choice), sequences.equalsOperator);
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
        Sequences<T> disjunct = splitDisjunct(candidates, soFar);
        if (!disjunct.isEmpty()) {
            return disjunct;
        } else {
            return splitCommon(candidates, soFar);
        }
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || stream().allMatch(Sequence::isEmpty);
    }

    private Sequences<T> splitDisjunct(List<List<Sequences<T>>> candidates, List<Sequences<T>> soFar) {
        Sequences<T> disjunct = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (int i = 0; i < size(); i++) {
            disjunct.add(null);
        }

        int length = 1;
        while (!isEmpty()) {
            SequenceLookup<T> distinct = new SequenceLookup<>(size());
            distinct.scan(this, length);

            boolean elementRemoved = false;
            for (int i = 0; i < size(); i++) {
                Sequence<T> sequence = get(i);
                if (!sequence.isEmpty()) {
                    T element = sequence.get(0);
                    if (!othersStartWith(sequence, element)) {
                        if (distinct.occursInAnotherDistinctSequence(element)) {
                            List<Sequences<T>> candidate = clone(soFar);

                            Sequences<T> current = new Sequences<>(disjunct);
                            current.get(i, () -> new Sequence<>(equalsOperator)).add(element);
                            current = new Sequences<>(
                                    current.stream().filter(Sequence::nonEmpty).collect(Collectors.toList()),
                                    equalsOperator, joinCommonOperator, joinSequenceOperator);
                            candidate.add(current);

                            Sequences<T> withoutElement = new Sequences<>(this);
                            withoutElement.get(i).remove(element);
                            List<Sequences<T>> slices = slice(candidates, withoutElement);
                            candidate.addAll(slices);
                            candidates.add(candidate);
                        } else {
                            disjunct.get(i, () -> new Sequence<>(equalsOperator)).add(element);
                            sequence.remove(element);
                            elementRemoved = true;
                        }
                    }
                }
            }
            if (!elementRemoved) {
                break;
            }
        }

        Sequences<T> slice = createDisjunctSlice(disjunct);
        return slice;
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
                .map(joinSequenceOperator::apply).map(Sequence::new).collect(Collectors.toList());
        Sequences<T> sliced = new Sequences<>(elements, equalsOperator, joinCommonOperator, joinSequenceOperator);
        return sliced;
    }

    public static <T> List<Sequences<T>> reduce(List<List<Sequences<T>>> candidates) {
        return candidates.stream().reduce((a, b) -> commonness(a) > commonness(b) ? a : b).orElseThrow();
    }

    public static <T> int commonness(List<Sequences<T>> sequences) {
        return sequences.stream().map(Sequences::commonness).reduce(Math::max).orElse(0);
    }

    private int commonness() {
        // TODO resolve cast to make class generic
        return stream().flatMap(Sequence::stream).map(element -> ((PhraseString) element).indices.size())
                .reduce(Math::max).orElse(0);
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
        Sequences<T> common = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (int n = 0; n < size(); n++) {
            common.add(new Sequence<>(equalsOperator));
        }

        Optional<Integer> maxCommon = maxCommon(0, 1);
        if (maxCommon.isPresent()) {
            int length = 1;
            while (true) {
                SequenceLookup<T> distinct = new SequenceLookup<>(size());
                distinct.scan(this, length);

                boolean nothingFound = collectCommonStartElements(common, length);
                if (nothingFound) {
                    break;
                } else {
                    Optional<Integer> nextMaxCommon = maxCommonAfter(common);
                    if (nextMaxCommon.isEmpty() || nextMaxCommon.get().equals(maxCommon.get())) {
                        Sequences<T> slice = new Sequences<>(this).gatherCommonElements(common);
                        // TODO 1-n combinations of common chunks
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

        Sequences<T> slice = gatherCommonSlice(common);
        return slice;
    }

    private List<Sequences<T>> sliceCommonWithoutStartElements(List<List<Sequences<T>>> candidates,
            List<Sequences<T>> soFar, Sequence<T> startElements) {
        List<Sequences<T>> candidate = clone(soFar);
        candidate.add(new Sequences<>(Collections.singleton(startElements), equalsOperator, joinCommonOperator,
                joinSequenceOperator));
        Sequences<T> withoutElement = new Sequences<>(this);
        for (Sequence<T> sequence : withoutElement) {
            if (sequence.startsWith(startElements)) {
                sequence.remove(0, startElements.size());
            }
        }

        List<Sequences<T>> slices = slice(candidates, withoutElement);
        candidate.addAll(slices);
        return candidate;
    }

    private boolean collectCommonStartElements(Sequences<T> common, int length) {
        boolean nothingFound = true;
        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            if (sequence.size() >= length) {
                List<T> startElements = sequence.subList(0, length);
                if (othersStartWith(sequence, startElements)) {
                    // common.set(i, new Sequence<>(joinSequenceOperator.apply(startElements)));
                    common.set(i, new Sequence<>(startElements));
                    nothingFound = false;
                }
            }
        }
        return nothingFound;
    }

    private Sequences<T> gatherCommonElements(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> distinct = distinct(candidates);
        Sequences<T> common = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
        distinct.values().stream().map(Sequence<T>::new).forEach(common::add);
        return common;
    }

    private Sequences<T> gatherCommonSlice(List<Sequence<T>> candidates) {
        Map<String, Sequence<T>> distinct = distinct(candidates);
        Sequences<T> common = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
        distinct.values().stream().map(joinSequenceOperator::apply).map(Sequence::new).forEach(common::add);
        return common;
    }

    private Optional<Integer> maxCommonAfter(List<Sequence<T>> sequences) {
        List<Integer> startIndices = sequences.stream()
                .map(sequence -> sequence != null && !sequence.isEmpty() ? sequence.size() : 0).collect(toList());
        Sequences<T> remaining = remaining(startIndices);
        return remaining.maxCommon(0, 1);
    }

    private Sequences<T> remaining(List<Integer> from) {
        Sequences<T> remaining = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);

        for (int i = 0; i < size(); i++) {
            Sequence<T> sequence = get(i);
            remaining.add(new Sequence<>(sequence.subList(from.get(i), sequence.size()), equalsOperator));
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
                Sequence<T> candidate = new Sequence<>(elements);
                if (!candidate.isEmpty()) {
                    // TODO 1.b resolve conflict with equalsOperator result, use type T instead of String
                    String key = joinSequenceOperator.apply(candidate).toString().toLowerCase();
                    if (reduced.containsKey(key)) {
                        Sequence<T> existing = reduced.get(key);
                        Sequence<T> joined = new Sequence<>(existing);
                        joined.addAll(candidate);
                        // reduced.put(key, new Sequence<>(
                        // joinCommonOperator.apply(Arrays.asList(candidate.get(0), existing.get(0)))));
                        Sequence<T> joinedElements = new Sequence<>(equalsOperator);
                        for (int j = 0; j < existing.size(); j++) {
                            joinedElements
                                    .add(joinCommonOperator.apply(Arrays.asList(candidate.get(j), existing.get(j))));
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

    private boolean othersStartWith(Sequence<T> skip, T element) {
        return stream().filter(seq -> seq != skip).anyMatch(seq -> seq.startsWith(element));
    }

    private boolean othersStartWith(Sequence<T> skip, List<T> elements) {
        return stream().filter(seq -> seq != skip).anyMatch(seq -> seq.startsWith(elements));
    }

    Sequence<T> commonStart() {
        Sequence<T> sequence = new Sequence<>(get(0), equalsOperator);
        for (int i = sequence.size(); i >= 1; --i) {
            if (allStartWithSequence(sequence)) {
                return common(sequence);
            }
            sequence.remove(sequence.size() - 1);
        }

        return new Sequence<>(Collections.emptyList(), equalsOperator);
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
        Sequence<T> sequence = new Sequence<>(get(0), equalsOperator);
        for (int i = sequence.size(); i >= 1; --i) {
            if (allEndWithSequence(sequence)) {
                return common(sequence);
            }
            sequence.remove(0);
        }

        return new Sequence<>(Collections.emptyList(), equalsOperator);
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
        Sequence<T> sequence = new Sequence<>(get(0), equalsOperator);

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

        return new Sequence<>(Collections.emptyList(), equalsOperator);
    }

    private Sequence<T> common(Sequence<T> candidate) {
        List<Sequence<T>> common = stream().map(s -> s.subList(candidate)).collect(toList());
        List<T> joined = new ArrayList<>(candidate.size());
        for (int i = 0; i < candidate.size(); i++) {
            final int index = i;
            List<T> slice = common.stream().map(e -> e.get(index)).collect(toList());
            joined.add(joinCommonOperator.apply(slice));
        }
        return new Sequence<>(joined, equalsOperator);
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

    public static <T> List<T> flatten(List<Sequences<T>> sliced, BiPredicate<T, T> equalsOperator,
            Function<List<T>, T> joinSequenceOperator) {
        int phraseCount = phraseCount(sliced);
        if (phraseCount == 0) {
            return Collections.emptyList();
        }

        List<T> flattened = new ArrayList<>(phraseCount);

        for (int phraseIndex = 0; phraseIndex < phraseCount; phraseIndex++) {
            Sequence<T> phrase = new Sequence<>(equalsOperator);

            for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
                Sequences<T> sequences = sliced.get(ruleIndex);
                Sequence<T> sequence = sequences.get(Math.min(phraseIndex, sequences.size() - 1));
                phrase.addAll(sequence);
            }

            flattened.add(joinSequenceOperator.apply(phrase));
        }

        return flattened;
    }

    public Sequences<T> joinWith(Sequences<T> second) {
        Sequences<T> joined = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
        if (this.size() > second.size()) {
            for (int i = 0; i < this.size(); i++) {
                List<T> elements = new ArrayList<>(get(i));
                elements.addAll(second.get(0));
                joined.add(new Sequence<>(joinSequenceOperator.apply(elements)));
            }
        } else if (this.size() < second.size()) {
            for (int i = 0; i < second.size(); i++) {
                List<T> elements = new ArrayList<>(get(0));
                elements.addAll(second.get(i));
                joined.add(new Sequence<>(joinSequenceOperator.apply(elements)));
            }
        } else {
            for (int i = 0; i < second.size(); i++) {
                List<T> elements = new ArrayList<>(get(i));
                elements.addAll(second.get(i));
                joined.add(new Sequence<>(joinSequenceOperator.apply(elements)));
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
