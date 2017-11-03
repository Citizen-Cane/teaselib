/**
 * 
 */
package teaselib.util.math;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class StatisticsTest {

    @Test
    public void testBasics() {
        Statistics<Double> statistics = new Statistics<>(new ArrayList<>());
        assertTrue(statistics.isEmpty());

        statistics.add(10.0);
        assertFalse(statistics.isEmpty());
        assertEquals(1, statistics.size());

        assertEquals(10.0, statistics.min(), 0.0);
        assertEquals(10.0, statistics.max(), 0.0);
        assertEquals(10.0, statistics.median(), 0.0);
        assertEquals(10.0, statistics.mean(), 0.0);

        assertEquals(0.0, statistics.deviation(), 0.0);
        assertEquals(0.0, statistics.variance(), 0.0);

        statistics.add(20.0);
        assertEquals(2, statistics.size());

        assertEquals(10.0, statistics.min(), 0.0);
        assertEquals(20.0, statistics.max(), 0.0);
        assertEquals(15.0, statistics.median(), 0.0);
        assertEquals(15.0, statistics.mean(), 0.0);

        assertEquals(5.0, statistics.deviation(), 0.0);
        assertEquals(25.0, statistics.variance(), 0.0);

        statistics.add(6.0);
        assertEquals(3, statistics.size());

        assertEquals(06.0, statistics.min(), 0.0);
        assertEquals(20.0, statistics.max(), 0.0);
        assertEquals(10.0, statistics.median(), 0.0);
        assertEquals(12.0, statistics.mean(), 0.0);

        assertEquals(5.88, statistics.deviation(), 0.01);
        assertEquals(34.66, statistics.variance(), 0.01);
    }

    @Test
    public void testMediaDoesntChangeValueOrder() {
        Statistics<Double> statistics = new Statistics<>(new ArrayList<>());

        statistics.add(10.0);
        statistics.add(20.0);
        statistics.add(6.0);

        assertEquals(10.0, statistics.median(), 0.0);
    }

    @Test
    public void testRemove() {
        Statistics<Double> statistics = new Statistics<>(new ArrayList<Double>());

        statistics.add(10.0);
        statistics.add(20.0);
        statistics.add(6.0);

        assertEquals(3, statistics.size());

        assertEquals(10.0, statistics.get(0), 0.0);
        assertEquals(20.0, statistics.get(1), 0.0);
        assertEquals(06.0, statistics.get(2), 0.0);

        statistics.removeValue(20.0);

        assertEquals(2, statistics.size());

        assertEquals(10.0, statistics.get(0), 0.0);
        assertEquals(06.0, statistics.get(1), 0.0);
    }

}
