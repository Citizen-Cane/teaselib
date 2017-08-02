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

    public static final double DEVIATION_ACCEPTED_GOOD_MIN_DEFAULT = 0.9;
    public static final double DEVIATION_ACCEPTED_GOOD_MAX_DEFAULT = 1.05;

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

    private final Statistics<Double> statistics = new Statistics<Double>();
    Result result = Result.ADJUSTING;

    int adjustingSize = ADJUSTING_SIZE_DEFAULT;
    int windowSize = WINDOW_SIZE_DEFAULT;

    private double acceptedMin = DEVIATION_ACCEPTED_GOOD_MIN_DEFAULT;
    private double acceptedMax = DEVIATION_ACCEPTED_GOOD_MAX_DEFAULT;

    private double acceptedLow = DEVIATION_LOW_BUT_ACCEPTED_DEFAULT;
    private double acceptedHigh = DEVIATION_HIGH_BUT_ACCEPTED_DEFAULT;

    private double tooLow = DEVIATION_TOO_LOW_DEFAULT;
    private double tooHigh = DEVIATION_TOO_HIGH_DEFAULT;

    public WindowedMean setAcceptedGood(double acceptedMin, double acceptedMax) {
        this.setAcceptedMin(acceptedMin);
        this.setAcceptedMax(acceptedMax);
        return this;
    }

    public WindowedMean setAcceptedHighLowLimits(double acceptedLow, double acceptedHigh) {
        this.setAcceptedLow(acceptedLow);
        this.setAcceptedHigh(acceptedHigh);
        return this;
    }

    public WindowedMean setTooHighTooLowLimits(double tooLow, double tooHigh) {
        this.setTooLow(tooLow);
        this.setTooHigh(tooHigh);
        return this;
    }

    public Result add(Double value) {
        double mean = statistics.mean();
        if (statistics.size() < adjustingSize) {
            result = Result.ADJUSTING;
        } else if (value > mean * getAcceptedHigh()) {
            result = Result.TOO_HIGH;
        } else if (value < mean * getAcceptedLow()) {
            result = Result.TOO_LOW;
        } else if (value > mean * getAcceptedGoodMax()) {
            result = Result.GOOD_HIGH;
        } else if (value < mean * getAcceptedGoodMin()) {
            result = Result.GOOD_LOW;
        } else {
            result = Result.GOOD_ACCEPTED;
        }

        if (result == Result.ADJUSTING) {
            statistics.add(value);
        } else if (mean * getTooLow() < value && value < mean * getTooHigh()) {
            statistics.add(value);
        }

        if (statistics.size() > windowSize) {
            statistics.remove(0);
        }

        return result;
    }

    public Statistics<Double> statistics() {
        return statistics;
    }

    public Result result() {
        return result;
    }

    public double getAcceptedHigh() {
        return acceptedHigh;
    }

    public void setAcceptedHigh(double acceptedHigh) {
        this.acceptedHigh = acceptedHigh;
    }

    public double getAcceptedLow() {
        return acceptedLow;
    }

    public void setAcceptedLow(double acceptedLow) {
        this.acceptedLow = acceptedLow;
    }

    public double getAcceptedGoodMax() {
        return acceptedMax;
    }

    public void setAcceptedMax(double acceptedMax) {
        this.acceptedMax = acceptedMax;
    }

    public double getAcceptedGoodMin() {
        return acceptedMin;
    }

    public void setAcceptedMin(double acceptedMin) {
        this.acceptedMin = acceptedMin;
    }

    public double getTooLow() {
        return tooLow;
    }

    public void setTooLow(double tooLow) {
        this.tooLow = tooLow;
    }

    public double getTooHigh() {
        return tooHigh;
    }

    public void setTooHigh(double tooHigh) {
        this.tooHigh = tooHigh;
    }

}
