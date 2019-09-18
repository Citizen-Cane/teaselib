package teaselib.core.speechrecognition.srgs;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
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

    private static <T> List<Sequences<T>> slice_old(Sequences<T> sequences, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator, UnaryOperator<T> emptyCloneOp) {
        List<Sequences<T>> slices = new ArrayList<>();

        // to pass unit test SpeechRecognitionComplexTest.testSliceDistinctChociesWithPairwiseCommonPartsShort:
        // TODO slice chunks with maximal partial common parts
        // -> return Sequences<T> commonStart with complete choice coverage
        // likewise commonMiddle
        Sequence<T> commonStart = sequences.commonStart();
        if (!commonStart.isEmpty()) {
            slices.add(new Sequences<>(Collections.singletonList(commonStart), sequences.equalsOperator,
                    sequences.joinCommonOperator, sequences.joinSequenceOperator));
        }
        Sequences<T> remainder = commonStart.isEmpty() ? sequences : sequences.removeIncluding(commonStart);

        while (remainder.maxLength() > 0) {

            // to pass unit test SpeechRecognitionComplexTest.testSliceDistinctChociesWithPairwiseCommonPartsShort:
            // TODO slice chunks with maximal partial common parts
            Sequence<T> commonMiddle = remainder.commonMiddle();

            if (!commonMiddle.isEmpty()) {
                Sequences<T> unique = remainder.removeUpTo(commonMiddle, emptyCloneOp);
                slices.add(new Sequences<>(unique, sequences.equalsOperator, joinCommonOperator, joinSequenceOperator));
                slices.add(new Sequences<>(Collections.singletonList(commonMiddle), sequences.equalsOperator,
                        sequences.joinCommonOperator, sequences.joinSequenceOperator));
            }

            if (commonMiddle.isEmpty()) {
                slices.add(remainder);
                break;
            }
            remainder = remainder.removeIncluding(commonMiddle);
        }
        return slices;
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
            disjunct.add(new Sequence<>(equalsOperator));
        }

        // What's wrong?
        // splits from single sequence as long as the element to split doens't occur in otrher sequence
        // -> removes a lot of entries from sequence
        // TODO If all first elements are distinct, remove them

        // TODO Remove element if it occurs only once in the set of first elements
        // -> remove one element after another until only common elements are left
        // TODO do this column wise (all first elements, then next
        while (!isEmpty()) {
            boolean elementRemoved = false;
            for (int i = 0; i < size(); i++) {
                Sequence<T> sequence = get(i);
                if (!sequence.isEmpty()) {
                    T element = sequence.get(0);
                    if (!othersStartWith(sequence, element)) {
                        disjunct.get(i).add(element);
                        sequence.remove(element);
                        elementRemoved = true;
                    }
                }
            }
            if (!elementRemoved) {
                break;
            }
        }

        return disjunct;
    }

    private Sequences<T> splitCommon() {
        // TODO Find common sequences, add as single new sequences with combined choice indices

        List<T> candidates = new ArrayList<>(size());
        for (int n = 0; n < size(); n++) {
            candidates.add(null);
        }

        int length = 1;
        while (true) {
            boolean nothingFound = true;
            Sequences<T> sequences = this;
            for (int i = 0; i < sequences.size(); i++) {
                Sequence<T> sequence = sequences.get(i);
                if (sequence.size() >= length) {
                    List<T> element = sequence.subList(0, length);
                    for (int j = 0; j < size(); j++) {
                        Sequence<T> otherSequence = get(j);
                        if (sequence != otherSequence) {
                            if (otherSequence.size() >= length) {
                                List<T> otherElement = otherSequence.subList(0, length);
                                if (element.toString().equals(otherElement.toString())) {
                                    candidates.set(i, joinSequenceOperator.apply(element));
                                    nothingFound = false;
                                }
                            }
                        }
                    }
                }
            }

            if (nothingFound) {
                break;
            } else {
                length++;
            }
        }

        // only valid candidates or null

        Map<String, T> reduced = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            T value = candidates.get(i);
            if (value != null) {
                String key = value.toString();
                if (reduced.containsKey(key)) {
                    T existing = reduced.get(key);
                    reduced.put(key, joinCommonOperator.apply(Arrays.asList(existing, value)));
                } else {
                    reduced.put(key, value);
                }

                // TODO Generalize
                int l = value.toString().split(" ").length;
                get(i).remove(0, l);
            }
        }

        Sequences<T> common = new Sequences<>(equalsOperator, joinCommonOperator, joinSequenceOperator);
        reduced.values().stream().map(Sequence<T>::new).forEach(common::add);
        return common;
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

    Sequences<T> removeUpTo(Sequence<T> match, UnaryOperator<T> emptyCloneOp) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (Sequence<T> listSequence : this) {
            List<T> subList = listSequence.subList(0, listSequence.indexOf(match));
            if (subList.isEmpty()) {
                subList = singletonList(emptyCloneOp.apply(listSequence.get(0)));
            }
            subLists.add(new Sequence<>(subList, equalsOperator));
        }
        return subLists;
    }

    Sequences<T> removeExcluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (Sequence<T> listSequence : this) {
            List<T> subList = listSequence.subList(0, listSequence.indexOf(match));
            subLists.add(new Sequence<>(subList, equalsOperator));
        }
        return subLists;
    }

    Sequences<T> removeIncluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (Sequence<T> listSequence : this) {
            List<T> subList = listSequence.subList(listSequence.indexOf(match) + match.size(), listSequence.size());
            subLists.add(new Sequence<>(subList, equalsOperator));
        }
        return subLists;
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
