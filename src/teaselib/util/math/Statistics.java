package teaselib.util.math;

import java.util.Arrays;
import java.util.List;

/**
 * derived from <a href=
 * "http://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-median-and-standard-deviation-in-c-or-java/7988556">
 * http://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-
 * median-and-standard-deviation-in-c-or-java/7988556</a>
 *
 */
public class Statistics {
    final Number[] data;
    final int size;

    public Statistics(Number[] data) {
        this.data = data;
        size = data.length;
    }

    public Statistics(List<? extends Number> data) {
        Number[] array = new Number[data.size()];
        this.data = data.toArray(array);
        size = data.size();
    }

    public double mean() {
        double sum = 0.0;
        for (Number a : data)
            sum += a.doubleValue();
        return sum / size;
    }

    public double variance() {
        double mean = mean();
        double temp = 0;
        for (Number a : data) {
            double d = mean - a.doubleValue();
            temp += d * d;
        }
        return temp / size;
    }

    public double deviation() {
        return Math.sqrt(variance());
    }

    public double median() {
        Arrays.sort(data);
        if (data.length % 2 == 0) {
            return (data[(size / 2) - 1].doubleValue()
                    + data[size / 2].doubleValue()) / 2.0;
        } else {
            return data[size / 2].doubleValue();
        }
    }
}
