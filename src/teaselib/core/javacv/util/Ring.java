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
    private Supplier<T> supplier;

    Ring(Supplier<T> supplier, int size) {
        this.supplier = supplier;
        elements = new ArrayDeque<>(size);
        resize(size);
    }

    public void resize(int size) {
        elements.clear();
        for (int i = 0; i < size; i++) {
            elements.addLast(this.supplier.get());
        }
    }

    T getCurrent() {
        return elements.getFirst();
    }

    T getLast() {
        return elements.getLast();
    }

    void advance() {
        elements.addFirst(elements.removeLast());
    }
}
