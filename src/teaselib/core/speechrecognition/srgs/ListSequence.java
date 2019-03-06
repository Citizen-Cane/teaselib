package teaselib.core.speechrecognition.srgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListSequence<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    public ListSequence(T[] elements) {
        this(Arrays.asList(elements));
    }

    public ListSequence(List<? extends T> elements) {
        super(elements);
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
            if (!get(index + i).equals(elements.get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<ListSequence<T>> subLists() {
        List<ListSequence<T>> candidates = new ArrayList<>();
        for (int n = size(); n > 0; --n) {
            for (int i = 0; i <= size() - n; ++i) {
                candidates.add(new ListSequence<>(this.subList(i, i + n)));
            }
        }
        return candidates;
    }

    @Override
    public String toString() {
        return stream().map(T::toString).collect(Collectors.joining(" "));
    }
}
