package teaselib.util.math;

/**
 * Weight normals for [0,1] -> [0-1]. All functions are defined for [0,1] only.
 * 
 * @author Citizen-Cane
 *
 */
public class WeightNormal {

    private WeightNormal() { //
    }

    public static float linear(float x) {
        return x;
    }

    public static float square(float x) {
        return -x * x + 2 * x;
    }

}
