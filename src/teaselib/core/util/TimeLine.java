/**
 * 
 */
package teaselib.core.util;

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
    private final LinkedList<Double> timeSpans = new LinkedList<Double>();

    private int maxItems = 1000;
    private double maxTimeSpanSeconds = 60.0;

    private T last = null;
    private double tailTime;

    public TimeLine() {
        tailTime = System.currentTimeMillis() / 1000.0;
    }

    public TimeLine(double startTime) {
        tailTime = startTime;
    }

    public void clear() {
        items.clear();
        timeSpans.clear();
    }

    public void setCapacity(int items, double timeSpanSeconds) {
        maxItems = items;
        maxTimeSpanSeconds = timeSpanSeconds;
    }

    public boolean add(T item, double timeStamp) {
        boolean different = (last == null && item != null)
                || !item.equals(last);
        if (different) {
            items.add(item);
            timeSpans.add(timeStamp - tailTime);
            tailTime = timeStamp;
            ensureCapacity();
        } else {
            final int size = timeSpans.size();
            if (size == 0) {
                timeSpans.add(timeStamp - tailTime);
            } else {
                timeSpans.set(size - 1, timeStamp - tailTime);
            }
            // Capacity doesn't change
        }
        last = item;
        return different;
    }

    private void ensureCapacity() {
        // TODO store timestamps instead of delta - saves summing up
        double sum = sum(timeSpans);
        if (sum > maxTimeSpanSeconds) {
            removeFirstSeconds(sum - maxTimeSpanSeconds);
        } else if (timeSpans.size() > maxItems) {
            removeFirstN(timeSpans.size() - maxItems);
        }
    }

    private static double sum(List<Double> values) {
        double sum = 0.0;
        for (double d : values) {
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

    private void removeFirstSeconds(double timeSpanSeconds) {
        for (double i = 0; i < timeSpanSeconds && i < items.size();) {
            items.removeFirst();
            double d = timeSpans.removeFirst();
            i += d;
        }
    }

    public int size() {
        return items.size();
    }

    public T tail() {
        return items.getLast();
    }

    public List<T> tail(double timeSpanSeconds) {
        List<T> tail = new Vector<T>(10);
        Iterator<T> item = items.descendingIterator();
        Iterator<Double> timeSpan = timeSpans.descendingIterator();
        while (item.hasNext()) {
            tail.add(item.next());
            double t = timeSpan.next();
            if (t < timeSpanSeconds) {
                timeSpanSeconds -= t;
            } else {
                break;
            }
        }
        return tail;
    }
}
