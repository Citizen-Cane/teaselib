package teaselib.util;

import java.util.Iterator;

public class Interval implements Iterable<Integer> {
    public final int start;
    public final int end;

    public Interval(int start, int end) {
        if (start > end)
            throw new IllegalArgumentException("start > end");
        this.start = start;
        this.end = end;
    }

    public float average() {
        return start / 2.0f + end / 2.0f;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int i = start;

            @Override
            public boolean hasNext() {
                return i <= end;
            }

            @Override
            public Integer next() {
                return Integer.valueOf(i++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public boolean contains(Interval other) {
        return start <= other.start && other.end <= end;
    }

    public boolean contains(int n) {
        return start <= n && n <= end;
    }

    public boolean contains(float f) {
        return start <= f && f <= end;
    }

    public boolean contains(Integer n) {
        return start <= n && n <= end;
    }

    public int size() {
        return end - start;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + end;
        result = prime * result + start;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Interval other = (Interval) obj;
        if (end != other.end)
            return false;
        if (start != other.start)
            return false;
        return true;
    }

}