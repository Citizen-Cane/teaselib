package teaselib.util.math;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class Hysteresis {

    private Hysteresis() { //
    }

    public static Function<Boolean, Float> bool(UnaryOperator<Float> hysteresis, float max, float min) {
        return new Function<Boolean, Float>() {

            UnaryOperator<Float> function = hysteresis;

            @Override
            public Float apply(Boolean value) {
                return function.apply(value.booleanValue() ? max : min);
            }

        };
    }

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
