package teaselib.util.math;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.util.Interval;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RandomTest {
    private static final double Exact = 0.0;

    @Test
    public void testChance() {
        Random random = new Random();
        assertFalse(random.chance(0.0f));
        Assertions.assertTrue(random.chance(1.0f));
    }

    @Test
    public void testThatRandomNumbersIncludeMinAndMax() {
        Random random = new Random();

        int min = 1;
        int max = 100;
        Interval interval = new Interval(min, max);
        for (int i : interval) {
            int n = random.value(min, i);
            Assertions.assertTrue(n >= min);
            Assertions.assertTrue(n <= max);
        }
    }

    @Test
    public void testScaleDouble() {
        Random random = new Random();

        Assertions.assertEquals(0.0, random.scale(0, 0.0, 100.0), Exact);
        Assertions.assertEquals(50.0, random.scale(0.5, 0.0, 100.0), Exact);
        Assertions.assertEquals(100.0, random.scale(1.0, 0.0, 100.0), Exact);
    }
}
