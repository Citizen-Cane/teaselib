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
    private final transient Function<? super T, String> elementToString;

    @SafeVarargs
    public Sequence(T... elements) {
        this(Arrays.asList(elements), Object::equals);
    }

    public Sequence(T[] elements, BiPredicate<T, T> equals) {
        this(Arrays.asList(elements), equals);
    }

    public Sequence(T[] elements, BiPredicate<T, T> equals, Function<T, String> elementToString) {
        this(Arrays.asList(elements), equals, elementToString);
    }

    public Sequence(T[] elements, Function<T, String> elementToString) {
        this(Arrays.asList(elements), T::equals, elementToString);
    }

    public Sequence(List<T> elements, BiPredicate<T, T> equals) {
        this(elements, equals, T::toString);
    }

    public Sequence(List<T> elements, Function<T, String> elementToString) {
        super(elements);
        this.equalsOperator = T::equals;
        this.elementToString = elementToString;
    }

    public Sequence(List<T> elements, BiPredicate<T, T> equals, Function<T, String> elementToString) {
        super(elements);
        this.equalsOperator = equals;
        this.elementToString = elementToString;
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

    public List<Sequence<T>> subLists() {
        List<Sequence<T>> candidates = new ArrayList<>();
        for (int n = size(); n > 0; --n) {
            for (int i = 0; i <= size() - n; ++i) {
                candidates.add(new Sequence<>(this.subList(i, i + n), this.equalsOperator));
            }
        }
        return candidates;
    }

    @Override
    public String toString() {
        return stream().map(elementToString).collect(Collectors.joining(" "));
    }

}
