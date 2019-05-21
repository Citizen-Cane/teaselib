package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    final transient BiPredicate<T, T> equalsOperator;
    final transient Function<List<T>, T> joinCommonOperator;
    final transient Function<List<T>, T> joinSequenceOperator;

    public Sequences(BiPredicate<T, T> equals, Function<List<T>, T> joinOp, Function<List<T>, T> joinSequenceOperator) {
        super();
        this.equalsOperator = equals;
        this.joinCommonOperator = joinOp;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public Sequences(Collection<? extends Sequence<T>> elements, BiPredicate<T, T> equals,
            Function<List<T>, T> joinCommonOperator, Function<List<T>, T> joinSequenceOperator) {
        super(elements);
        this.equalsOperator = equals;
        this.joinCommonOperator = joinCommonOperator;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public Sequences(int initialCapacity, BiPredicate<T, T> equals, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator) {
        super(initialCapacity);
        this.equalsOperator = equals;
        this.joinCommonOperator = joinCommonOperator;
        this.joinSequenceOperator = joinSequenceOperator;
    }

    public static <T> List<Sequences<T>> of(Iterable<T> elements, BiPredicate<T, T> equalsOp,
            Function<T, List<T>> splitter, Function<List<T>, T> joinCommonOperator,
            Function<List<T>, T> joinSequenceOperator, UnaryOperator<T> emptyCloneOp) {
        Iterator<T> choices = elements.iterator();
        if (!choices.hasNext()) {
            return Collections.emptyList();
        } else {
            Sequences<T> sequences = new Sequences<>(equalsOp, joinCommonOperator, joinSequenceOperator);
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

        Sequence<T> commonStart = sequences.commonStart();
        if (!commonStart.isEmpty()) {
            slices.add(new Sequences<T>(Collections.singletonList(commonStart), sequences.equalsOperator,
                    sequences.joinCommonOperator, sequences.joinSequenceOperator));
        }
        Sequences<T> remainder = commonStart.isEmpty() ? sequences : sequences.removeIncluding(commonStart);

        while (remainder.maxLength() > 0) {
            Sequence<T> commonMiddle = remainder.commonMiddle();
            if (!commonMiddle.isEmpty()) {
                Sequences<T> unique = remainder.removeUpTo(commonMiddle, emptyCloneOp);
                slices.add(
                        new Sequences<T>(unique, sequences.equalsOperator, joinCommonOperator, joinSequenceOperator));
                slices.add(new Sequences<T>(Collections.singletonList(commonMiddle), sequences.equalsOperator,
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

    public Sequence<T> commonStart() {
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

    public Sequence<T> commonEnd() {
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

    public Sequence<T> commonMiddle() {
        Sequence<T> sequence = new Sequence<>(get(0), equalsOperator);
        List<Sequence<T>> candidates = sequence.subLists();

        for (Sequence<T> candidate : candidates) {
            if (allContainMiddleSequence(candidate)) {
                return common(candidate);
            }
        }

        return new Sequence<>(Collections.emptyList(), equalsOperator);
    }

    private Sequence<T> common(Sequence<T> candidate) {
        List<Sequence<T>> common = stream().map(s -> s.subList(candidate)).collect(Collectors.toList());
        List<T> joined = new ArrayList<>(candidate.size());
        for (int i = 0; i < candidate.size(); i++) {
            final int index = i;
            List<T> slice = common.stream().map(e -> e.get(index)).collect(Collectors.toList());
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

    public Sequences<T> removeUpTo(Sequence<T> match, UnaryOperator<T> emptyCloneOp) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (Sequence<T> listSequence : this) {
            List<T> subList = listSequence.subList(0, listSequence.indexOf(match));
            if (subList.isEmpty()) {
                subList = Collections.singletonList(emptyCloneOp.apply(listSequence.get(0)));
            }
            subLists.add(new Sequence<>(subList, equalsOperator));
        }
        return subLists;
    }

    public Sequences<T> removeExcluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator, joinCommonOperator, joinSequenceOperator);
        for (Sequence<T> listSequence : this) {
            List<T> subList = listSequence.subList(listSequence.indexOf(match), listSequence.size());
            subLists.add(new Sequence<>(subList, equalsOperator));
        }
        return subLists;
    }

    public Sequences<T> removeIncluding(Sequence<T> match) {
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

    public static <T> int phraseCount(List<Sequences<T>> phrases) {
        return phrases.stream().map(List::size).reduce(Math::max).orElse(0);
    }

    public int maxLength() {
        Optional<? extends Sequence<T>> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    public List<String> toStrings() {
        return stream().map(Sequence::toString).collect(Collectors.toList());
    }

    public static <T> List<T> flatten(List<Sequences<T>> sliced, BiPredicate<T, T> equalsOp,
            BinaryOperator<T> concatOperator) {
        int phraseCount = phraseCount(sliced);
        if (phraseCount == 0) {
            return Collections.emptyList();
        }

        List<T> flattened = new ArrayList<>(phraseCount);

        for (int phraseIndex = 0; phraseIndex < phraseCount; phraseIndex++) {
            Sequence<T> phrase = new Sequence<>(equalsOp);

            for (int ruleIndex = 0; ruleIndex < sliced.size(); ruleIndex++) {
                Sequences<T> sequences = sliced.get(ruleIndex);
                Sequence<T> sequence = sequences.get(Math.min(phraseIndex, sequences.size() - 1));
                phrase.addAll(sequence);
            }

            flattened.add(phrase.join(concatOperator));
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
        } else {
            for (int i = 0; i < second.size(); i++) {
                List<T> elements = new ArrayList<>(get(0));
                elements.addAll(second.get(i));
                joined.add(new Sequence<>(joinSequenceOperator.apply(elements)));
            }
        }
        return joined;
    }
}
