package teaselib.util.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.Test;

import teaselib.util.Interval;

public class RandomTest {
    private static final double Exact = 0.0;

    @Test
    public void testChance() {
        Random random = new Random();
        assertFalse(random.chance(0.0f));
        assertTrue(random.chance(1.0f));
    }

    @Test
    public void testThatRandomNumbersIncludeMinAndMax() {
        Random random = new Random();

        int min = 1;
        int max = 100;
        Interval interval = new Interval(min, max);
        for (int i : interval) {
            int n = random.value(min, i);
            assertTrue(n >= min);
            assertTrue(n <= max);
        }
    }

    @Test
    public void testScaleDouble() {
        Random random = new Random();

        assertEquals(0.0, random.scale(0, 0.0, 100.0), Exact);
        assertEquals(50.0, random.scale(0.5, 0.0, 100.0), Exact);
        assertEquals(100.0, random.scale(1.0, 0.0, 100.0), Exact);
    }
}
