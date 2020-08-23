package teaselib.util.math;

public class Unit {

    public static final double PI = java.lang.Math.PI;

    private Unit() { //
    }

    public static float degrees(float rad) {
        return (float) (rad * 180.0f / PI);
    }

    public static double degrees(double rad) {
        return rad * 180.0d / PI;
    }

    public static float rad(float degrees) {
        return (float) (degrees * PI / 180.0f);
    }

    public static double rad(double degrees) {
        return degrees * PI / 180.0d;
    }

}
