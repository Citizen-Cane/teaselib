package teaselib.util.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.util.Interval;

public class RandomTest {
    private static final double Exact = 0.0;

    @Test
    public void testThatRandomNumbersIncludeMinAndMax() {
        int min = 1;
        int max = 100;
        Interval interval = new Interval(min, max);
        for (int n : interval) {
            int random = Random.random(min, n);
            assertTrue(random >= min);
            assertTrue(random <= max);
        }
    }

    @Test
    public void testTransformNormalizedValueToIntervalDouble() {
        assertEquals(0.0,
                Random.transformNormalizedValueToInterval(0.0, 100.0, 0),
                Exact);

        assertEquals(50.0,
                Random.transformNormalizedValueToInterval(0.0, 100.0, 0.5),
                Exact);

        assertEquals(100.0,
                Random.transformNormalizedValueToInterval(0.0, 100.0, 1.0),
                Exact);
    }
}
