package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class Sequence<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    static class Traits<T> {
        final BiPredicate<T, T> equalsOperator;
        final Function<T, List<T>> splitter;
        final Function<List<T>, T> joinCommonOperator;
        final ToIntFunction<T> commonnessOperator;
        final Function<List<T>, T> joinSequenceOperator;
        final BiPredicate<List<T>, List<T>> joinableSequences;
        final BiPredicate<List<T>, List<T>> joinablePhrases;

        Traits(BiPredicate<T, T> equalsOperator, Function<T, List<T>> splitter, ToIntFunction<T> commonnessOperator,
                Function<List<T>, T> joinCommonOperator, Function<List<T>, T> joinSequenceOperator,
                BiPredicate<List<T>, List<T>> joinableSequences, BiPredicate<List<T>, List<T>> joinablePhrases) {
            this.equalsOperator = equalsOperator;
            this.splitter = splitter;
            this.commonnessOperator = commonnessOperator;
            this.joinCommonOperator = joinCommonOperator;
            this.joinSequenceOperator = joinSequenceOperator;
            this.joinableSequences = joinableSequences;
            this.joinablePhrases = joinablePhrases;
        }
    }

    final Sequence.Traits<T> traits;

    public static <T> Sequence<T> of(T t, Traits<T> traits) {
        return new Sequence<>(traits.splitter.apply(t), traits);
    }

    public Sequence(Sequence<T> elements) {
        this(elements, elements.traits);
    }

    public Sequence(Traits<T> traits) {
        super();
        this.traits = traits;
    }

    public Sequence(T element, Traits<T> traits) {
        this(Collections.singletonList(element), traits);
    }

    public Sequence(List<T> elements, Traits<T> traits) {
        super(elements);
        this.traits = traits;
    }

    public boolean startsWith(List<? extends T> elements) {
        return matchesAt(elements, 0);
    }

    /**
     * @param elements
     * @param index
     *            ranging from 0 to size() - elements.size()
     * @return
     */
    public boolean matchesAt(List<? extends T> elements, int index) {
        if (elements.size() + index > size()) {
            return false;
        }

        for (int i = 0; i < elements.size(); i++) {
            if (!traits.equalsOperator.test(get(index + i), elements.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean mergeableWith(Sequence<T> other) {
        return traits.joinablePhrases.test(this, other);
    }

    public List<T> subList(int from) {
        return subList(from, size());
    }

    public boolean nonEmpty() {
        return size() > 0;
    }

    @Override
    public String toString() {
        return stream().map(T::toString).filter(t -> !t.isEmpty()).collect(Collectors.joining(" "));
    }

    public void remove(int from, int size) {
        for (int i = 0; i < size; i++) {
            remove(from);
        }
    }

    public T joined() {
        return traits.joinSequenceOperator.apply(this);
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
        if (!(obj instanceof Sequence))
            return false;
        return true;
    }

}
