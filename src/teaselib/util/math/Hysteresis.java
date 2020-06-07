package teaselib.util.math;

import java.util.function.UnaryOperator;

public class Hysteresis {

    public static UnaryOperator<Float> function(float start, float up, float down) {
        return new UnaryOperator<Float>() {

            float v = start;

            @Override
            public Float apply(Float x) {
                v = clamp(start + x * up - (1.0f - x) * down);
                return v;
            }

        };
    }

    static float clamp(float x) {
        if (x < 0.0)
            return 0.0f;
        if (x > 1.0)
            return 1.0f;
        return x;
    }
}
