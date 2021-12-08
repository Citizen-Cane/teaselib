package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class Sequence<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    static class Traits<T> {
        final Comparator<T> comparator;
        final Comparator<List<T>> listComparator;
        final Function<T, List<T>> splitter;
        final Function<List<T>, T> joinCommonOperator;
        final ToIntFunction<T> commonnessOperator;
        final Function<List<T>, T> joinSequenceOperator;
        final BiPredicate<List<T>, List<T>> joinableSequences;
        final BiPredicate<List<T>, List<T>> joinablePhrases;
        final BiPredicate<T, T> intersectionPredicate;

        Traits(Comparator<T> comperator, Function<T, List<T>> splitter, ToIntFunction<T> commonnessOperator,
                Function<List<T>, T> joinCommonOperator, Function<List<T>, T> joinSequenceOperator,
                BiPredicate<List<T>, List<T>> joinableSequences, BiPredicate<List<T>, List<T>> joinablePhrases,
                BiPredicate<T, T> intersectionPredicate) {
            this.comparator = comperator;
            this.listComparator = this::compare;
            this.splitter = splitter;
            this.commonnessOperator = commonnessOperator;
            this.joinCommonOperator = joinCommonOperator;
            this.joinSequenceOperator = joinSequenceOperator;
            this.joinableSequences = joinableSequences;
            this.joinablePhrases = joinablePhrases;
            this.intersectionPredicate = intersectionPredicate;
        }

        int compare(List<T> s1, List<T> s2) {
            if (s1 == s2)
                return 0;
            int size1 = s1.size();
            int size2 = s2.size();
            if (size1 != size2) {
                return size2 - size1;
            } else {
                for (int i = 0; i < size1; i++) {
                    int c = comparator.compare(s1.get(i), s2.get(i));
                    if (c != 0) {
                        return c;
                    }
                }
                return 0;
            }
        }

    }

    final Sequence.Traits<T> traits;

    public static <T> Sequence<T> of(T t, Traits<T> traits) {
        return new Sequence<>(traits.splitter.apply(t), traits);
    }

    public static <T> Sequence<T> of(List<T> t, Traits<T> traits) {
        return new Sequence<>(t, traits);
    }

    public Sequence(Sequence<T> elements) {
        this(elements, elements.traits);
    }

    public Sequence(Traits<T> traits) {
        this.traits = traits;
    }

    public Sequence(Traits<T> traits, int capacity) {
        super(capacity);
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
        int size = elements.size();
        if (size + index > size()) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            if (traits.comparator.compare(get(index + i), elements.get(i)) != 0) {
                return false;
            }
        }
        return true;
    }

    public int indexOf(T element, int start) {
        int size = size();
        for (int i = start; i < size; ++i) {
            if (traits.comparator.compare(get(i), element) == 0) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(List<? extends T> elements) {
        return indexOf(elements, 0);
    }

    public int indexOf(List<? extends T> elements, int start) {
        int size = size();
        int limit = size - elements.size();
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Empty sequence");
        } else if (start > limit) {
            return -1;
        }

        for (int i = start; i <= limit; ++i) {
            if (matchesAt(elements, i)) {
                return i;
            }
        }
        return -1;
    }

    public boolean joinableSequences(Sequence<T> other) {
        return traits.joinableSequences.test(this, other);
    }

    public boolean joinablePhrase(Sequence<T> other) {
        return traits.joinablePhrases.test(this, other);
    }

    public int maxCommonness() {
        int max = Integer.MIN_VALUE;
        for (T element : this) {
            max = Math.max(max, traits.commonnessOperator.applyAsInt(element));
        }
        return max;
    }

    public static <T> Sequence<T> maxLength(Sequence<T> a, Sequence<T> b) {
        return a.size() > b.size() ? a : b;
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
        if (size() == 1) {
            return get(0);
        } else {
            return traits.joinSequenceOperator.apply(this);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public int compareTo(Sequence<T> other) {
        int size = size();
        int otherSize = other.size();
        if (size != otherSize) {
            return otherSize - size;
        } else {
            for (int i = 0; i < size; i++) {
                int compare = traits.comparator.compare(get(i), other.get(i));
                if (compare != 0) {
                    return compare;
                }
            }
            return 0;
        }
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
