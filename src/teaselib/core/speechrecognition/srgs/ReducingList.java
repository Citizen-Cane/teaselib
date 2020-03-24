package teaselib.core.speechrecognition.srgs;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

class ReducingList<T> implements List<T> {
    private final BiFunction<T, T, T> reducer;
    private T element = null;
    private int index = 0;
    private int reduced = 0;

    public ReducingList(BinaryOperator<T> reducer) {
        this.reducer = reducer;
    }

    @Override
    public int size() {
        return element != null ? 1 : 0;
    }

    @Override
    public boolean isEmpty() {
        return element == null;
    }

    @Override
    public boolean contains(Object o) {
        return element == o;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean hasNext = getResult() != null;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public T next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                } else {
                    hasNext = false;
                    return getResult();
                }
            }

        };
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("hiding")
    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T e) {
        if (element == null)
            element = e;
        else {
            element = reducer.apply(element, e);
            if (element == e) {
                reduced++;
            }
        }
        index++;
        return element == e;
    }

    @Override
    public boolean remove(Object o) {
        if (element != null && element == o) {
            element = null;
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        T old = element;
        c.stream().forEach(this::add);
        return element != old;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (0 > index || index > 0)
            throw new IndexOutOfBoundsException(index);
        return addAll(c);
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    public boolean removeAll(Collection<?> c) {
        T old = element;
        c.stream().forEach(this::remove);
        return element != old;
    }

    @Override
    @SuppressWarnings("unlikely-arg-type")
    public boolean retainAll(Collection<?> c) {
        if (c.contains(element))
            return false;
        else
            element = null;
        return true;
    }

    @Override
    public void clear() {
        element = null;
    }

    @Override
    public T get(int index) {
        if (0 > index || index > 0)
            throw new IndexOutOfBoundsException(index);
        return element;
    }

    @Override
    public T set(int index, T element) {
        if (0 > index || index > 0)
            throw new IndexOutOfBoundsException(index);
        T old = element;
        this.element = reducer.apply(this.element, element);
        return old;
    }

    @Override
    public void add(int index, T element) {
        if (0 > index || index > 0)
            throw new IndexOutOfBoundsException(index);
        add(element);
    }

    @Override
    public T remove(int index) {
        if (0 > index || index > 0)
            throw new IndexOutOfBoundsException(index);
        T old = element;
        this.element = null;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        if (element == o) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == element)
            return 0;
        else
            return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if (0 > fromIndex || fromIndex > 0)
            throw new IndexOutOfBoundsException(fromIndex);
        if (toIndex == 0) {
            return Collections.emptyList();
        } else if (toIndex <= size()) {
            return Collections.singletonList(element);
        } else {
            throw new IndexOutOfBoundsException(toIndex);
        }
    }

    public T getResult() {
        return element;
    }

    @Override
    public String toString() {
        return "index = " + index + ", reduced = " + reduced + ", current = " + element;
    }

}
