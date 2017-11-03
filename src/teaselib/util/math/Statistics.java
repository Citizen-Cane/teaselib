package teaselib.util.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * derived from <a href=
 * "http://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-median-and-standard-deviation-in-c-or-java/7988556">
 * http://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-
 * median-and-standard-deviation-in-c-or-java/7988556</a>
 *
 */
public class Statistics<N extends Number & Comparable<N>> {
    final List<N> values;

    public Statistics() {
        this.values = new ArrayList<>();
    }

    public Statistics(N[] values) {
        this.values = Arrays.asList(values);
    }

    public Statistics(List<N> values) {
        this.values = values;
    }

    public void add(N value) {
        values.add(value);
    }

    public N get(int index) {
        return values.get(index);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * The mean value is the average of the numbers.
     * 
     * @return
     */
    public double mean() {
        double sum = 0.0;
        for (Number a : values)
            sum += a.doubleValue();
        int size = values.size();
        return sum / size;
    }

    /**
     * Variance.
     * 
     * @return
     */
    public double variance() {
        double mean = mean();
        double temp = 0;
        for (Number a : values) {
            double d = mean - a.doubleValue();
            temp += d * d;
        }
        int size = values.size();
        return temp / size;
    }

    /**
     * Standard deviation.
     * 
     * @return
     */
    public double deviation() {
        return Math.sqrt(variance());
    }

    /**
     * The "middle" of a sorted list of values.
     * 
     * @return
     */
    public double median() {
        ArrayList<N> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1).doubleValue() + sorted.get(size / 2).doubleValue()) / 2.0;
        } else {
            return sorted.get(size / 2).doubleValue();
        }
    }

    public double min() {
        double min = Double.MAX_VALUE;
        for (Number a : values) {
            if (a.doubleValue() < min) {
                min = a.doubleValue();
            }
        }
        return min;
    }

    public double max() {
        double max = -Double.MAX_VALUE;
        for (Number a : values) {
            if (a.doubleValue() > max) {
                max = a.doubleValue();
            }
        }
        return max;
    }

    /**
     * Remove th element with the supplied value
     * 
     * @param value
     */
    public void removeValue(N value) {
        values.remove(value);
    }

    public void remove(int index) {
        values.remove(index);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
