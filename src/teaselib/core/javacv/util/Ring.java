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

    public T getCurrent() {
        return elements.getFirst();
    }

    public T getLast() {
        return elements.getLast();
    }

    public void advance() {
        elements.addFirst(elements.removeLast());
    }

    public int size() {
        return elements.size();
    }
}
