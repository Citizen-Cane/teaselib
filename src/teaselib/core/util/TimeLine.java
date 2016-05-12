/**
 * 
 */
package teaselib.core.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Associates objects with time stamps. Objects can be retrieved based on the
 * elapsed duration.
 * <p>
 * New items are only added if they're different from the last. If they're the
 * same, only the time span is updated.
 */
public class TimeLine<T> {
    private final LinkedList<T> items = new LinkedList<T>();
    private final LinkedList<Long> timeSpans = new LinkedList<Long>();
    private int maxItems = 1000;
    private long maxTimeSpanMillis = 60 * 1000;

    private T last = null;
    private long tailTimeMillis;

    public TimeLine() {
        tailTimeMillis = System.currentTimeMillis();
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
        add(last, timeStamp);
        return false;
    }

    public boolean add(T item, double timeStampSeconds) {
        return add(item, (long) (timeStampSeconds * 1000));
    }

    public boolean add(T item, long timeStamp) {
        boolean different = last == null && item == null ? false
                : (last == null && item != null)
                        || (last != null && item == null) || !item.equals(last);
        if (different) {
            items.add(item);
            timeSpans.add(timeStamp - tailTimeMillis);
            tailTimeMillis = timeStamp;
            ensureCapacity();
        } else {
            final int size = timeSpans.size();
            if (size == 0) {
                timeSpans.add(timeStamp - tailTimeMillis);
            } else {
                timeSpans.set(size - 1, timeStamp - tailTimeMillis);
            }
            // Capacity doesn't change
        }
        last = item;
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
        for (long i = 0; i < timeSpanMillis && i < items.size();) {
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

    public List<T> getTimeSpan(double timeSpanSeconds) {
        List<T> tail = new Vector<T>(10);
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

    public List<T> last(int n) {
        int size = items.size();
        if (size == 0)
            return Collections.EMPTY_LIST;
        else
            return items.subList(Math.max(0, size - n), size);
    }
}
