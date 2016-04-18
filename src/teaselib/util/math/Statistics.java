package teaselib.util.math;

import java.util.List;

public class Statistics {
    public static double mean(List<Long> values) {
        long value = 0;
        for (Long d : values) {
            value += d;
        }
        value /= 10;
        return value;
    }
}
