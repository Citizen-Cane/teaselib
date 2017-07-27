/**
 * 
 */
package teaselib.util.math;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class StatisticsThrowAndRetrieveTest {
    static final String ADJUSTING = "ADJUSTING";
    static final String GOOD_SAME = "GOOD_SAME";
    static final String GOOD_SLOW = "GOOD_SLOW";
    static final String GOOD_FAST = "GOOD_FAST";
    static final String TOO_SLOW = "TOO_SLOW";
    static final String TOO_FAST = "TOO_FAST";

    private static final double DEVIATION_HIGH_BUT_ACCEPTED = 1.15;
    private static final double DEVIATION_TOO_HIGH = 1.3;

    private static final double DEVIATION_LOW_BUT_ACCEPTED = 0.5;
    private static final double DEVIATION_TOO_LOW = 0.5;

    private static final double EPSILON_SAME_MIN = 0.9;
    private static final double EPSILON_SAME_MAX = 1.05;

    private static final int ADJUSTING_SIZE = 3;
    private static final int WINDOW_SIZE = 3;

    @Test
    public void simulateThrowRetrieveTiming() {
        assertEquals(
                Arrays.asList(ADJUSTING, ADJUSTING, ADJUSTING, GOOD_SAME, TOO_SLOW, GOOD_FAST, GOOD_SAME, TOO_SLOW),
                simulateThrowAndRetrieveTiming(new Double[] { 5.0, 10.0, 12.0, 9.0, 13.0, 8.0, 9.0, 12.0 }));
    }

    @Test
    public void simulateThrowRetrieveTiming2() {
        assertEquals(Arrays.asList(ADJUSTING, ADJUSTING, ADJUSTING, GOOD_FAST, GOOD_FAST, GOOD_SLOW, TOO_SLOW),
                simulateThrowAndRetrieveTiming(new Double[] { 12.0, 10.0, 8.0, 6.0, 5.0, 7.0, 8.0 }));
    }

    private static List<String> simulateThrowAndRetrieveTiming(Double[] values) {
        Statistics<Double> statistics = new Statistics<Double>(new ArrayList<Double>());
        List<String> actual = new ArrayList<String>();
        for (Double value : values) {
            if (statistics.size() < ADJUSTING_SIZE) {
                actual.add(ADJUSTING);
                statistics.add(value);
            } else {
                double mean = statistics.mean();
                if (value > mean * DEVIATION_HIGH_BUT_ACCEPTED) {
                    actual.add(TOO_SLOW);
                } else if (value < mean * DEVIATION_LOW_BUT_ACCEPTED) {
                    actual.add(TOO_FAST);
                } else if (value > mean * EPSILON_SAME_MAX) {
                    actual.add(GOOD_SLOW);
                } else if (value < mean * EPSILON_SAME_MIN) {
                    actual.add(GOOD_FAST);
                } else {
                    actual.add(GOOD_SAME);
                }

                if (mean * DEVIATION_TOO_LOW < value && value < mean * DEVIATION_TOO_HIGH) {
                    statistics.add(value);
                }

                if (statistics.size() > WINDOW_SIZE) {
                    statistics.remove(0);
                }
            }
        }
        return actual;
    }
}
