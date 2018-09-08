/**
 * 
 */
package teaselib.stimulation;

/**
 * @author Citizen-Cane
 *
 */
public class SquareWave extends WaveForm {

    public SquareWave(long onTimeMillis, long offTimeMillis) {
        if (onTimeMillis > 0) {
            add(MAX, onTimeMillis);
        }
        if (offTimeMillis > 0) {
            add(MIN, offTimeMillis);
        }
    }

    public SquareWave(double onTimeSeconds, double offTimeSeconds) {
        this(toMillis(onTimeSeconds), toMillis(offTimeSeconds));
    }

    public static SquareWave get(double periodSeconds, double onPercentage) {
        double onDuration = periodSeconds * clamp(onPercentage);
        return new SquareWave(onDuration, periodSeconds - onDuration);
    }

}
