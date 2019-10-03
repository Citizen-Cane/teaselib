package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

        Traits(BiPredicate<T, T> equalsOperator, Function<T, List<T>> splitter, ToIntFunction<T> commonnessOperator,
                Function<List<T>, T> joinCommonOperator, Function<List<T>, T> joinSequenceOperator) {
            this.equalsOperator = equalsOperator;
            this.splitter = splitter;
            this.commonnessOperator = commonnessOperator;
            this.joinCommonOperator = joinCommonOperator;
            this.joinSequenceOperator = joinSequenceOperator;
        }
    }

    final Sequence.Traits<T> traits;

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
        return indexOf(elements) == 0;
    }

    public boolean endsWith(List<? extends T> elements) {
        return lastIndexOf(elements) == size() - elements.size();
    }

    public int indexOf(List<? extends T> elements) {
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Empty sequence");
        }

        for (int i = 0; i <= size() - elements.size(); ++i) {
            if (matchesAt(elements, i)) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(List<? extends T> elements) {
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Empty sequence");
        }

        for (int i = size() - elements.size(); i >= 0; --i) {
            if (matchesAt(elements, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param elements
     * @param index
     *            ranging from 0 to size() - elements.size()
     * @return
     */
    private boolean matchesAt(List<? extends T> elements, int index) {
        for (int i = 0; i < elements.size(); i++) {
            if (!traits.equalsOperator.test(get(index + i), elements.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all sub-sequences, starting with the largest
     * 
     * @return
     */
    public List<Sequence<T>> subLists() {
        List<Sequence<T>> candidates = new ArrayList<>();
        for (int n = size(); n > 0; --n) {
            for (int i = 0; i <= size() - n; ++i) {
                candidates.add(new Sequence<>(this.subList(i, i + n), traits));
            }
        }
        return candidates;
    }

    public Sequence<T> subList(Sequence<T> list) {
        int from = indexOf(list);
        return new Sequence<>(subList(from, from + list.size()), traits);
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

    public T join(Function<List<T>, T> joinSequenceOperator) {
        return joinSequenceOperator.apply(this);
    }

    public void remove(int from, int size) {
        for (int i = 0; i < size; i++) {
            remove(from);
        }
    }

    public boolean startsWith(T element) {
        if (isEmpty())
            return false;
        return traits.equalsOperator.test(get(0), element);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(traits.equalsOperator);
        return result;
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
