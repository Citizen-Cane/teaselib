package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class Sequences<T> extends ArrayList<Sequence<T>> {
    private static final long serialVersionUID = 1L;

    final transient BiPredicate<T, T> equalsOperator;

    public Sequences() {
        this(T::equals);
    }

    public Sequences(BiPredicate<T, T> equals) {
        super();
        this.equalsOperator = equals;
    }

    public Sequences(Sequence<T>[] elements, BiPredicate<T, T> equals) {
        this(Arrays.asList(elements), equals);
    }

    public Sequences(Collection<? extends Sequence<T>> elements, BiPredicate<T, T> equals) {
        super(elements);
        this.equalsOperator = equals;
    }

    public Sequences(int initialCapacity, BiPredicate<T, T> equals) {
        super(initialCapacity);
        this.equalsOperator = equals;
    }

    public Sequence<T> commonStart() {
        Sequence<T> sequence = new Sequence<>(get(0), equalsOperator);
        for (int i = sequence.size(); i >= 1; --i) {
            if (allStartWithSequence(sequence)) {
                return sequence;
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
                return sequence;
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
                return new Sequence<>(candidate, equalsOperator);
            }
        }

        return new Sequence<>(Collections.emptyList(), equalsOperator);
    }

    private boolean allContainMiddleSequence(Sequence<T> sequence) {
        for (int j = 1; j < size(); ++j) {
            if (get(j).indexOf(sequence) == -1) {
                return false;
            }
        }
        return true;
    }

    public Sequences<T> removeUpTo(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator);
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(listSequence.subList(0, listSequence.indexOf(match)), equalsOperator));
        }
        return subLists;
    }

    public Sequences<T> removeExcluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator);
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(listSequence.subList(listSequence.indexOf(match), listSequence.size()),
                    equalsOperator));
        }
        return subLists;
    }

    public Sequences<T> removeIncluding(Sequence<T> match) {
        Sequences<T> subLists = new Sequences<>(size(), equalsOperator);
        for (Sequence<T> listSequence : this) {
            subLists.add(new Sequence<>(
                    listSequence.subList(listSequence.indexOf(match) + match.size(), listSequence.size()),
                    equalsOperator));
        }
        return subLists;
    }

    public boolean containsOptionalParts() {
        return stream().filter(Sequence<T>::isEmpty).count() > 0;
    }

    public int maxLength() {
        Optional<? extends Sequence<T>> reduced = stream().reduce((a, b) -> a.size() > b.size() ? a : b);
        return reduced.isPresent() ? reduced.get().size() : 0;
    }

    public List<String> toStrings() {
        return stream().map(Sequence::toString).collect(Collectors.toList());
    }

}
