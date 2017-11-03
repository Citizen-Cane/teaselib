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
 * @author Citizen-Cane
 *
 */
public class StatisticsThrowAndRetrieveTest {
    static final String ADJUSTING = "ADJUSTING";
    static final String GOOD_ACCEPTED = "GOOD_SAME";
    static final String GOOD_LOW = "GOOD_SLOW";
    static final String GOOD_HIGH = "GOOD_FAST";
    static final String TOO_LOW = "TOO_SLOW";
    static final String TOO_HIGH = "TOO_FAST";

    private static final double DEVIATION_HIGH_BUT_ACCEPTED = 1.15;
    private static final double DEVIATION_TOO_HIGH = 1.3;

    private static final double DEVIATION_LOW_BUT_ACCEPTED = 0.5;
    private static final double DEVIATION_TOO_LOW = 0.5;

    private static final double DEVIATION_ACCEPTED_MIN = 0.9;
    private static final double DEVIATION_ACCEPTED_MAX = 1.05;

    private static final int ADJUSTING_SIZE = 3;
    private static final int WINDOW_SIZE = 3;

    @Test
    public void simulateThrowRetrieveTiming() {
        assertEquals(
                Arrays.asList(ADJUSTING, ADJUSTING, ADJUSTING, GOOD_ACCEPTED, TOO_HIGH, GOOD_LOW, GOOD_ACCEPTED,
                        TOO_HIGH),
                simulateThrowAndRetrieveTiming(new Double[] { 5.0, 10.0, 12.0, 9.0, 13.0, 8.0, 9.0, 12.0 }));
    }

    @Test
    public void simulateThrowRetrieveTiming2() {
        assertEquals(Arrays.asList(ADJUSTING, ADJUSTING, ADJUSTING, GOOD_LOW, GOOD_LOW, GOOD_HIGH, TOO_HIGH),
                simulateThrowAndRetrieveTiming(new Double[] { 12.0, 10.0, 8.0, 6.0, 5.0, 7.0, 8.0 }));
    }

    private static List<String> simulateThrowAndRetrieveTiming(Double[] values) {
        Statistics<Double> statistics = new Statistics<>(new ArrayList<Double>());
        List<String> actual = new ArrayList<>();
        for (Double value : values) {
            String result = getResult(statistics, value);
            addResult(statistics, actual, value, result);
        }
        return actual;
    }

    private static void addResult(Statistics<Double> statistics, List<String> actual, Double value,
            final String result) {
        actual.add(result);
        double mean = statistics.mean();
        if (result == ADJUSTING) {
            statistics.add(value);
        } else if (mean * DEVIATION_TOO_LOW < value && value < mean * DEVIATION_TOO_HIGH) {
            statistics.add(value);
        }

        if (statistics.size() > WINDOW_SIZE) {
            statistics.remove(0);
        }
    }

    private static String getResult(Statistics<Double> statistics, Double value) {
        final String result;
        double mean = statistics.mean();
        if (statistics.size() < ADJUSTING_SIZE) {
            result = ADJUSTING;
        } else if (value > mean * DEVIATION_HIGH_BUT_ACCEPTED) {
            result = TOO_HIGH;
        } else if (value < mean * DEVIATION_LOW_BUT_ACCEPTED) {
            result = TOO_LOW;
        } else if (value > mean * DEVIATION_ACCEPTED_MAX) {
            result = GOOD_HIGH;
        } else if (value < mean * DEVIATION_ACCEPTED_MIN) {
            result = GOOD_LOW;
        } else {
            result = GOOD_ACCEPTED;
        }
        return result;
    }

}
