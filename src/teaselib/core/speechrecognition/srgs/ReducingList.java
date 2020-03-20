package teaselib.core.speechrecognition.srgs;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

class ReducingList<T> implements List<T> {
    private final BiFunction<T, T, T> reducer;
    private T element = null;

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
        }
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

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        element = null;
    }

    @Override
    public T get(int index) {
        if (index > 0)
            throw new IndexOutOfBoundsException(index);
        return element;
    }

    @Override
    public T set(int index, T element) {
        if (index >= 0)
            throw new IndexOutOfBoundsException(index);
        T old = element;
        this.element = element;
        return old;
    }

    @Override
    public void add(int index, T element) {
        if (index >= 0)
            throw new IndexOutOfBoundsException(index);
        this.element = element;
    }

    @Override
    public T remove(int index) {
        if (index >= 0)
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    public T getResult() {
        return element;
    }

}
