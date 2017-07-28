/**
 * 
 */
package teaselib.util.math;

/**
 * @author Citizen-Cane
 *
 */
public class WindowedMean {
    public static final double DEVIATION_HIGH_BUT_ACCEPTED_DEFAULT = 1.15;
    public static final double DEVIATION_TOO_HIGH_DEFAULT = 1.3;

    public static final double DEVIATION_LOW_BUT_ACCEPTED_DEFAULT = 0.5;
    public static final double DEVIATION_TOO_LOW_DEFAULT = 0.5;

    public static final double DEVIATION_ACCEPTED_MIN_DEFAULT = 0.9;
    public static final double DEVIATION_ACCEPTED_MAX_DEFAULT = 1.05;

    public static final int ADJUSTING_SIZE_DEFAULT = 3;
    public static final int WINDOW_SIZE_DEFAULT = 3;

    public enum Result {
        ADJUSTING,
        GOOD_ACCEPTED,
        GOOD_LOW,
        GOOD_HIGH,
        TOO_LOW,
        TOO_HIGH,
    }

    Statistics<Double> statistics = new Statistics<Double>();

    int adjustingSize = ADJUSTING_SIZE_DEFAULT;
    int windowSize = WINDOW_SIZE_DEFAULT;

    double acceptedMin = DEVIATION_ACCEPTED_MIN_DEFAULT;
    double acceptedMax = DEVIATION_ACCEPTED_MAX_DEFAULT;

    double acceptedLow = DEVIATION_LOW_BUT_ACCEPTED_DEFAULT;
    double acceptedHigh = DEVIATION_HIGH_BUT_ACCEPTED_DEFAULT;

    double tooLow = DEVIATION_TOO_LOW_DEFAULT;
    double tooHigh = DEVIATION_TOO_HIGH_DEFAULT;

    public WindowedMean setAccepted(double acceptedMin, double acceptedMax) {
        this.acceptedMin = acceptedMin;
        this.acceptedMax = acceptedMax;
        return this;
    }

    public WindowedMean setHighLowLimits(double acceptedLow, double acceptedHigh) {
        this.acceptedLow = acceptedLow;
        this.acceptedHigh = acceptedHigh;
        return this;
    }

    public WindowedMean setTooHighTooLowLimits(double tooLow, double tooHigh) {
        this.tooLow = tooLow;
        this.tooHigh = tooHigh;
        return this;
    }

    public Result add(Double value) {
        final Result result;
        double mean = statistics.mean();
        if (statistics.size() < ADJUSTING_SIZE_DEFAULT) {
            result = Result.ADJUSTING;
        } else if (value > mean * DEVIATION_HIGH_BUT_ACCEPTED_DEFAULT) {
            result = Result.TOO_HIGH;
        } else if (value < mean * DEVIATION_LOW_BUT_ACCEPTED_DEFAULT) {
            result = Result.TOO_LOW;
        } else if (value > mean * DEVIATION_ACCEPTED_MAX_DEFAULT) {
            result = Result.GOOD_HIGH;
        } else if (value < mean * DEVIATION_ACCEPTED_MIN_DEFAULT) {
            result = Result.GOOD_LOW;
        } else {
            result = Result.GOOD_ACCEPTED;
        }

        if (result == Result.ADJUSTING) {
            statistics.add(value);
        } else if (mean * DEVIATION_TOO_LOW_DEFAULT < value && value < mean * DEVIATION_TOO_HIGH_DEFAULT) {
            statistics.add(value);
        }

        if (statistics.size() > WINDOW_SIZE_DEFAULT) {
            statistics.remove(0);
        }

        return result;
    }
}
