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
public class WindowedMeanTest {

    @Test
    public void simulateThrowRetrieve() {
        assertEquals(
                Arrays.asList(WindowedMean.Result.ADJUSTING, WindowedMean.Result.ADJUSTING,
                        WindowedMean.Result.ADJUSTING, WindowedMean.Result.GOOD_ACCEPTED, WindowedMean.Result.TOO_HIGH,
                        WindowedMean.Result.GOOD_LOW, WindowedMean.Result.GOOD_ACCEPTED, WindowedMean.Result.TOO_HIGH),
                simulateThrowAndRetrieveTiming(new Double[] { 5.0, 10.0, 12.0, 9.0, 13.0, 8.0, 9.0, 12.0 }));
    }

    @Test
    public void simulateThrowRetrieve2() {
        assertEquals(
                Arrays.asList(WindowedMean.Result.ADJUSTING, WindowedMean.Result.ADJUSTING,
                        WindowedMean.Result.ADJUSTING, WindowedMean.Result.GOOD_LOW, WindowedMean.Result.GOOD_LOW,
                        WindowedMean.Result.GOOD_HIGH, WindowedMean.Result.TOO_HIGH),
                simulateThrowAndRetrieveTiming(new Double[] { 12.0, 10.0, 8.0, 6.0, 5.0, 7.0, 8.0 }));
    }

    private static List<WindowedMean.Result> simulateThrowAndRetrieveTiming(Double[] values) {
        WindowedMean stats = new WindowedMean();

        List<WindowedMean.Result> actual = new ArrayList<>();
        for (Double value : values) {
            actual.add(stats.add(value));
        }
        return actual;
    }

}
