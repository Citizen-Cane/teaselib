package teaselib.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Associates objects with time stamps. Objects can be retrieved based on the elapsed duration.
 * <p>
 * New items are only added if they're different from the last. If they're the same, only the time span is updated.
 * 
 * @author Citizen-Cane
 */
public class TimeLine<T> {
    private int maxItems = 1000;
    private long maxTimeSpanMillis = 1000 * 60;

    private final LinkedList<T> items = new LinkedList<>();
    private final LinkedList<Long> timeSpans = new LinkedList<>();

    private T tail = null;
    private long tailTimeMillis;

    public TimeLine() {
        tailTimeMillis = 0;
    }

    public TimeLine(long startTimeMillis) {
        tailTimeMillis = startTimeMillis;
    }

    public TimeLine(double startTimeSeconds) {
        tailTimeMillis = (long) (startTimeSeconds * 1000);
    }

    public void clear() {
        items.clear();
        timeSpans.clear();
        tail = null;
        tailTimeMillis = 0;
    }

    public void setCapacity(int items, double timeSpanSeconds) {
        maxItems = items;
        maxTimeSpanMillis = (long) (timeSpanSeconds * 1000);
    }

    public void setCapacity(int items, long timeSpanMillis) {
        maxItems = items;
        maxTimeSpanMillis = timeSpanMillis;
    }

    public boolean add(double timeStamp) {
        return add(tail, timeStamp);
    }

    public boolean add(long timeStamp) {
        return add(tail, timeStamp);
    }

    public boolean add(T item, double timeStampSeconds) {
        return add(item, (long) (timeStampSeconds * 1000));
    }

    public boolean add(T item, long timeStamp) {
        boolean different = tail == null && item == null ? false
                : (tail == null && item != null) || (tail != null && item == null) || !item.equals(tail);
        if (different) {
            items.add(item);
            timeSpans.add(timeStamp - tailTimeMillis);
            ensureCapacity();
        } else {
            final int size = timeSpans.size();
            if (size == 0) {
                timeSpans.add(timeStamp - tailTimeMillis);
            } else {
                int headIndex = size - 1;
                timeSpans.set(headIndex, timeSpans.get(headIndex) + timeStamp - tailTimeMillis);
            }
            // Capacity doesn't change
        }
        tailTimeMillis = timeStamp;
        tail = item;
        return different;
    }

    private void ensureCapacity() {
        // TODO store timestamps instead of delta - saves summing up
        long sum = sum(timeSpans);
        if (sum > maxTimeSpanMillis) {
            removeFirstMillis(sum - maxTimeSpanMillis);
        } else if (timeSpans.size() > maxItems) {
            removeFirstN(timeSpans.size() - maxItems);
        }
    }

    private static long sum(List<Long> values) {
        long sum = 0;
        for (long d : values) {
            sum += d;
        }
        return sum;
    }

    private void removeFirstN(int n) {
        for (int i = 0; i < n; i++) {
            items.removeFirst();
            timeSpans.removeFirst();
        }
    }

    private void removeFirstMillis(long timeSpanMillis) {
        long i = 0;
        while (i < timeSpanMillis && i < items.size()) {
            items.removeFirst();
            long d = timeSpans.removeFirst();
            i += d;
        }
    }

    public int size() {
        return items.size();
    }

    public T tail() {
        return items.getLast();
    }

    public long tailTimeMillis() {
        return tailTimeMillis;
    }

    public List<T> getTimeSpan(double timeSpanSeconds) {
        List<T> tail = new ArrayList<>();
        Iterator<T> item = items.descendingIterator();
        Iterator<Long> timeSpan = timeSpans.descendingIterator();
        long timeSpanMillis = (long) (timeSpanSeconds * 1000);
        while (item.hasNext()) {
            tail.add(item.next());
            long t = timeSpan.next();
            if (t < timeSpanMillis) {
                timeSpanMillis -= t;
            } else {
                break;
            }
        }
        return tail;
    }

    public static class Slice<T> {
        public final long t;
        public final T item;

        public Slice(long t, T item) {
            this.t = t;
            this.item = item;
        }

        @Override
        public String toString() {
            return t + "->" + item;
        }
    }

    public List<Slice<T>> getTimeSpanSlices(double timeSpanSeconds) {
        return getTimeSpanSlices((long) (timeSpanSeconds * 1000));
    }

    public List<Slice<T>> getTimeSpanSlices(long timeSpanMillis) {
        if (timeSpanMillis == 0.0) {
            return Collections.emptyList();
        }
        List<Slice<T>> tail = new ArrayList<>();
        Iterator<T> item = items.descendingIterator();
        Iterator<Long> timeSpan = timeSpans.descendingIterator();
        while (item.hasNext()) {
            long t = timeSpan.next();
            tail.add(new Slice<>(Math.min(timeSpanMillis, t), item.next()));
            if (t < timeSpanMillis) {
                timeSpanMillis -= t;
            } else {
                break;
            }
        }
        return tail;
    }

    public List<T> last(int n) {
        int size = items.size();
        if (size == 0)
            return Collections.emptyList();
        else
            return items.subList(Math.max(0, size - n), size);
    }

    public List<Slice<T>> lastSlices(int n) {
        int size = items.size();
        if (size == 0) {
            return Collections.emptyList();
        } else {
            List<Slice<T>> tail = new ArrayList<>(n);
            Iterator<T> item = items.descendingIterator();
            Iterator<Long> timeSpan = timeSpans.descendingIterator();
            for (int i = 0; item.hasNext() && i < n; i++) {
                tail.add(new Slice<>(timeSpan.next(), item.next()));
            }

            return tail;
        }
    }

    public long duration(List<Slice<T>> slices) {
        long duration = 0;
        for (Slice<?> slice : slices) {
            duration += slice.t;
        }
        return duration;
    }

    public long tailTimeSpan() {
        if (timeSpans.isEmpty()) {
            return 0;
        } else {
            return timeSpans.getLast();
        }
    }

    public Slice<T> tailSlice() {
        return new Slice<>(tailTimeSpan(), tail());
    }
}
