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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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

    public static <T> List<Sequences<T>> of(Iterable<T> elements, BiPredicate<T, T> equalsOperator,
            Function<T, List<T>> splitter, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator, UnaryOperator<T> emptyCloneOp) {
        Iterator<T> choices = elements.iterator();
        if (!choices.hasNext()) {
            return Collections.emptyList();
        } else {
            Sequences<T> sequences = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
            for (T choice : elements) {
                Sequence<T> e = new Sequence<>(splitter.apply(choice), sequences.equalsOperator);
                sequences.add(e);
            }
            return slice(sequences, joinCommonOperator, joinSequenceOperator, emptyCloneOp);
        }
    }

    private static <T> List<Sequences<T>> slice(Sequences<T> sequences, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator, UnaryOperator<T> emptyCloneOp) {
        List<Sequences<T>> slices = new ArrayList<>();
        while (sequences.maxLength() > 0) {
            slices.add(sequences.slice());
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
     *         <li>return maximum reduceed part with minmized size
     * 
     */
    Sequences<T> slice() {
        Sequences<T> disjunct = splitDisjunct();
        if (!disjunct.isEmpty()) {
            return disjunct;
        } else {
            return splitCommon();
        }
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || stream().allMatch(Sequence::isEmpty);
    }

    private Sequences<T> splitDisjunct() {
        Sequences<T> disjunct = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (int i = 0; i < size(); i++) {
            disjunct.add(null);
        }

        int length = 1;
        while (!isEmpty()) {
            Map<String, AtomicInteger> distinct = new HashMap<>(size());
            Map<String, List<Sequence<T>>> lookup = new HashMap<>(size());
            stream().filter(seq -> seq.size() > 0).forEach(sequence -> {
                String key = new Sequence<>(sequence.subList(0, Math.min(length, sequence.size()))).toString()
                        .toLowerCase();
                distinct.computeIfAbsent(key, t -> new AtomicInteger(0)).incrementAndGet();
                lookup.computeIfAbsent(key, t -> new ArrayList<>()).add(sequence);
            });

            boolean elementRemoved = false;
            for (int i = 0; i < size(); i++) {
                Sequence<T> sequence = get(i);
                if (!sequence.isEmpty()) {
                    T element = sequence.get(0);
                    if (!othersStartWith(sequence, element)) {
                        String key = element.toString().toLowerCase();
                        AtomicInteger n = distinct.get(key);
                        if (n != null && n.intValue() == 1 && distinct.entrySet().stream()
                                .filter(entry -> entry.getValue().intValue() > 1).noneMatch(entry -> {
                                    return lookup.get(entry.getKey()).stream().anyMatch(seq -> {
                                        return seq.toString().toLowerCase().contains(key);
                                    });
                                })) {
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

        List<Sequence<T>> elements = disjunct.stream().filter(Objects::nonNull).map(joinSequenceOperator::apply)
                .map(Sequence::new).collect(Collectors.toList());
        return new Sequences<>(elements, equalsOperator, joinCommonOperator, joinSequenceOperator);
    }

    private Sequence<T> get(int index, Supplier<Sequence<T>> supplier) {
        Sequence<T> sequence = get(index);
        if (sequence == null) {
            sequence = supplier.get();
            set(index, sequence);
        }
        return sequence;
    }

    private Sequences<T> splitCommon() {
        List<T> candidates = new ArrayList<>(size());
        for (int n = 0; n < size(); n++) {
            candidates.add(null);
        }

        Optional<Integer> maxCommon = maxCommon(0, 1);
        if (maxCommon.isPresent()) {
            int length = 1;
            while (true) {
                boolean nothingFound = true;
                Sequences<T> sequences = this;
                exit: for (int i = 0; i < sequences.size(); i++) {
                    Sequence<T> sequence = sequences.get(i);
                    if (sequence.size() >= length) {
                        List<T> startElements = sequence.subList(0, length);
                        T newElement = startElements.get(startElements.size() - 1);
                        for (int j = 0; j < size(); j++) {
                            Sequence<T> otherSequence = get(j);
                            if (sequence != otherSequence && otherSequence.size() >= length) {
                                List<T> otherStartElements = otherSequence.subList(0, length);
                                boolean isStartOfOtherCommonChunk = startElements.size() > 1 //
                                        // TODO equalsOperator
                                        && !startElements.toString().equalsIgnoreCase(otherStartElements.toString())
                                        && equalsOperator.test(newElement, otherStartElements.get(0));
                                if (isStartOfOtherCommonChunk) {
                                    nothingFound = true;
                                    for (int k = 0; k < candidates.size(); k++) {
                                        // TODO store candidates as sequence elements, not joined
                                        if (candidates.get(k) != null && candidates.get(k).toString().toLowerCase()
                                                .startsWith(newElement.toString().toLowerCase())) {
                                            candidates.set(k, null);
                                        }
                                    }
                                    break exit;
                                } else if (startElements.toString().equalsIgnoreCase(otherStartElements.toString())) {
                                    // TODO 1.a Use the equalsOperator to compare lists elements
                                    candidates.set(i, joinSequenceOperator.apply(startElements));
                                    nothingFound = false;
                                }
                            }
                        }
                    }
                }

                if (nothingFound) {
                    break;
                } else {
                    Optional<Integer> nextMaxCommon = maxCommon(candidates);
                    if (nextMaxCommon.isEmpty() || nextMaxCommon.get().equals(maxCommon.get())) {
                        length++;
                    } else {
                        break;
                    }
                }
            }
        }

        Map<String, T> reduced = reduce(candidates);
        Sequences<T> common = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
        reduced.values().stream().map(Sequence<T>::new).forEach(common::add);
        return common;
    }

    private Optional<Integer> maxCommon(List<T> candidates) {
        // TODO Use generic split operator intead of toString()
        Sequences<T> remaining = remaining(
                candidates.stream().map(t -> t != null ? t.toString().split(" ").length : 0).collect(toList()));
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

    private Map<String, T> reduce(List<T> candidates) {
        Map<String, T> reduced = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            T candidate = candidates.get(i);
            if (candidate != null) {
                // TODO 1.b resolve conflict with equalsOperator result, use type T instead of String
                String key = candidate.toString().toLowerCase();
                if (reduced.containsKey(key)) {
                    T existing = reduced.get(key);
                    reduced.put(key, joinCommonOperator.apply(Arrays.asList(existing, candidate)));
                } else {
                    reduced.put(key, candidate);
                }

                // TODO Generalize and use op
                int l = candidate.toString().split(" ").length;
                get(i).remove(0, l);
            }
        }
        return reduced;
    }

    private boolean othersStartWith(Sequence<T> skip, T element) {
        return stream().filter(seq -> seq != skip).anyMatch(seq -> seq.startsWith(element));
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
