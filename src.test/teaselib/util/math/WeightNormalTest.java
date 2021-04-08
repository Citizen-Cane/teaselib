package teaselib.util.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class WeightNormalTest {

    @Test
    public void testLinear() throws Exception {
        assertEquals(0.00f, WeightNormal.linear(0.00f), 0.0f);
        assertEquals(0.25f, WeightNormal.linear(0.25f), 0.0f);
        assertEquals(0.50f, WeightNormal.linear(0.50f), 0.0f);
        assertEquals(0.75f, WeightNormal.linear(0.75f), 0.0f);
        assertEquals(1.00f, WeightNormal.linear(1.00f), 0.0f);

    }

    @Test
    public void testSquare() throws Exception {
        assertEquals(0.0000f, WeightNormal.square(0.00f), 0.0f);
        assertEquals(0.4375f, WeightNormal.square(0.25f), 0.0f);
        assertEquals(0.7500f, WeightNormal.square(0.50f), 0.0f);
        assertEquals(0.9375f, WeightNormal.square(0.75f), 0.0f);
        assertEquals(1.0000f, WeightNormal.square(1.00f), 0.0f);
    }

}
