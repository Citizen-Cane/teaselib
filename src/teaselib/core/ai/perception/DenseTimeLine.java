package teaselib.core.ai.perception;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author Citizen-Cane
 * 
 *         A timeLine that doesn't add duplicate elements. Instead, the timestamp is updated. As a result, the timeline
 *         always contains the latest occurrence of an element.
 *
 * @param <T>
 *            Element type.
 */
public class DenseTimeLine<T> {

    private final int capacity;
    private final long maxDurationMillis;

    public static class TimeStamp<T> {
        public final long timeMillis;
        public final long durationMillis;
        public final T element;

        public TimeStamp(T element, long time, long duration, TimeUnit unit) {
            super();
            this.timeMillis = MILLISECONDS.convert(time, unit);
            this.durationMillis = MILLISECONDS.convert(duration, unit);
            this.element = element;
        }

        @Override
        public String toString() {
            return element + " @ " + timeMillis + "ms + " + durationMillis;
        }

    }

    private final LinkedList<TimeStamp<T>> elements = new LinkedList<>();

    public DenseTimeLine() {
        this(10, 1, TimeUnit.SECONDS);
    }

    public DenseTimeLine(DenseTimeLine<T> other) {
        this(other.capacity, other.maxDurationMillis, MILLISECONDS, other.elements);
    }

    public DenseTimeLine(int capacity, long duration, TimeUnit unit) {
        this(capacity, duration, unit, Collections.emptyList());
    }

    private DenseTimeLine(int capacity, long duration, TimeUnit unit, List<TimeStamp<T>> elements) {
        this.capacity = capacity;
        this.maxDurationMillis = MILLISECONDS.convert(duration, unit);
        this.elements.addAll(elements);
    }

    public void clear() {
        elements.clear();
    }

    public boolean add(T element, long timestamp, TimeUnit unit) {
        TimeStamp<T> t = new TimeStamp<>(element, timestamp,
                elements.isEmpty() ? 0 : timestamp - elements.getLast().timeMillis, unit);
        boolean different = elements.isEmpty() || !elements.getLast().element.equals(element);
        if (different) {
            elements.addLast(t);
            ensureCapacity();
        } else {
            int last = elements.size() - 1;
            elements.set(last, t);
        }
        return different;
    }

    private void ensureCapacity() {
        long duration = elements.getLast().timeMillis - elements.getFirst().timeMillis;
        if (duration > maxDurationMillis) {
            removeFirst(duration - maxDurationMillis, MILLISECONDS);
        } else if (elements.size() > capacity) {
            removeFirst(elements.size() - capacity);
        }
    }

    private void removeFirst(long duration, TimeUnit unit) {
        long start = elements.getFirst().timeMillis;
        long durationMillis = MILLISECONDS.convert(duration, unit);
        while (1 < elements.size() && start + durationMillis > elements.getFirst().timeMillis) {
            elements.removeFirst();
        }
    }

    private void removeFirst(int n) {
        while (0 < n--) {
            elements.removeFirst();
        }
    }

    public T last() {
        return elements.getLast().element;
    }

    public TimeStamp<T> removeLast() {
        return elements.removeLast();
    }

    public DenseTimeLine<T> last(int n) {
        if (size() <= n) {
            return new DenseTimeLine<>(this);
        } else {
            return new DenseTimeLine<>(capacity, maxDurationMillis, MILLISECONDS, elements.subList(size() - n, size()));
        }
    }

    public DenseTimeLine<T> last(long duration, TimeUnit unit) {
        DenseTimeLine<T> tail = new DenseTimeLine<>();
        long durationMillis = MILLISECONDS.convert(duration, unit);
        Iterator<TimeStamp<T>> i = elements.descendingIterator();
        if (i.hasNext()) {
            TimeStamp<T> latest = i.next();
            tail.elements.addFirst(latest);
            while (i.hasNext()) {
                TimeStamp<T> t = i.next();
                if (latest.timeMillis - t.timeMillis < durationMillis) {
                    tail.elements.addFirst(t);
                }
            }
        }
        return tail;
    }

    long duration(TimeUnit unit) {
        return unit.convert(elements.getLast().timeMillis - elements.getFirst().timeMillis, MILLISECONDS);
    }

    public Stream<TimeStamp<T>> stream() {
        return elements.stream();
    }

    @Override
    public String toString() {
        return "[" + elements.size() + ", " + duration(MILLISECONDS) + "ms, " + timeline(elements) + "]";
    }

    private static <T> String timeline(List<TimeStamp<T>> elements) {
        List<String> timeline = new ArrayList<>(elements.size());
        Iterator<TimeStamp<T>> i = elements.iterator();
        if (i.hasNext()) {
            TimeStamp<T> timestamp = i.next();
            timeline.add(timestamp.element.toString() + "+" + 0);
            long start = timestamp.timeMillis;
            while (i.hasNext()) {
                timestamp = i.next();
                timeline.add(timestamp.element.toString() + "+" + (timestamp.timeMillis - start) + "ms");
            }
        }
        return timeline.toString();
    }

    public int size() {
        return elements.size();
    }

}
