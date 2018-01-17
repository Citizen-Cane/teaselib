package teaselib.core.javacv.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import teaselib.core.javacv.util.Buffer.Locked;

/**
 * @author Citizen-Cane
 *
 */
public class Buffer<T> implements Iterable<Locked<T>> {
    public static class Locked<T> {
        Semaphore lock = new Semaphore(1);
        T item;

        public Locked(T t) {
            this.item = t;
        }

        public T get() {
            lock.acquireUninterruptibly();
            return item;
        }

        public void release() {
            lock.release();
        }
    }

    List<Locked<T>> items;

    public Buffer(Supplier<T> supplier, int n) {
        super();
        this.items = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            items.add(new Locked<>(supplier.get()));
        }
    }

    public Locked<T> get(int i) {
        return items.get(i);
    }

    public Locked<T> get(T item) {
        for (Locked<T> locked : items) {
            if (locked.item == item) {
                return locked;
            }
        }
        throw new IllegalArgumentException(item.toString());
    }

    @Override
    public Iterator<Locked<T>> iterator() {
        return new Iterator<Locked<T>>() {
            int i = -1;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Locked<T> next() {
                i++;
                if (i == items.size()) {
                    i = 0;
                }
                return items.get(i);
            }

        };
    }
}
