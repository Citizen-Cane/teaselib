package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Sequence<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    final transient BiPredicate<T, T> equalsOperator;

    @SafeVarargs
    public Sequence(T... elements) {
        this(Arrays.asList(elements));
    }

    public Sequence(List<T> elements) {
        this(elements, Object::equals);
    }

    public Sequence(BiPredicate<T, T> equalsOperator) {
        super();
        this.equalsOperator = equalsOperator;
    }

    public Sequence(List<T> elements, BiPredicate<T, T> equalsOperator) {
        super(elements);
        this.equalsOperator = equalsOperator;
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
            if (!equalsOperator.test(get(index + i), elements.get(i))) {
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
                candidates.add(new Sequence<>(this.subList(i, i + n), this.equalsOperator));
            }
        }
        return candidates;
    }

    public Sequence<T> subList(Sequence<T> list) {
        int from = indexOf(list);
        return new Sequence<>(subList(from, from + list.size()));
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
        return equalsOperator.test(get(0), element);
    }

}
