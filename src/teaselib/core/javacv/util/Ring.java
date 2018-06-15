package teaselib.core.javacv.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * @author Citizen-Cane
 *
 */
public class Ring<T> {
    final Deque<T> elements;
    T previous;

    public Ring(Supplier<T> supplier, int size) {
        elements = new ArrayDeque<>(size);
        resize(supplier, size);
    }

    public void resize(Supplier<T> supplier, int size) {
        elements.clear();
        for (int i = 0; i < size; i++) {
            elements.addLast(supplier.get());
        }
    }

    public void start(Supplier<T> supplier) {
        for (int i = 0; i < elements.size(); i++) {
            elements.addLast(supplier.get());
        }
    }

    public T current() {
        return elements.getFirst();
    }

    public T previous() {
        return previous;
    }

    public T last() {
        return elements.getLast();
    }

    public void advance() {
        previous = current();
        elements.addFirst(elements.removeLast());
    }

    public int size() {
        return elements.size();
    }
}
