package teaselib.util.math;

public class Random {
    private static java.util.Random random = new java.util.Random(
            System.currentTimeMillis());

    /**
     * Return a random number
     * 
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @return A value in the interval [min, max]
     */
    public static int random(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static double random(double min, double max) {
        double r = random.nextDouble();
        return transformNormalizedValueToInterval(min, max, r);
    }

    public static double transformNormalizedValueToInterval(double min,
            double max, double r) {
        return min + (r * (max - min));
    }
}
