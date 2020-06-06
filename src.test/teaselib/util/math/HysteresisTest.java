package teaselib.util.math;

import static org.junit.Assert.assertEquals;

import java.util.function.UnaryOperator;

import org.junit.Test;

public class HysteresisTest {

    @Test
    public void test() {
        UnaryOperator<Float> f = Hysteresis.function(0.0f, 1.0f, 0.5f);

        float y1 = f.apply(0.0f);
        float y2 = f.apply(1.0f);
        float y3 = f.apply(1.0f);
        float y4 = f.apply(0.0f);
        float y5 = f.apply(1.0f);
        float y6 = f.apply(1.0f);
        float y7 = f.apply(0.0f);
        float y8 = f.apply(0.0f);

        assertEquals(0.0f, y1, 0.0);
        assertEquals(0.0f, y2, 1.0);
        assertEquals(0.0f, y3, 1.0);
        assertEquals(0.0f, y4, 0.5);
        assertEquals(0.0f, y5, 1.0);
        assertEquals(0.0f, y6, 1.0);
        assertEquals(0.0f, y7, 0.5);
        assertEquals(0.0f, y8, 0.0);
    }
}
