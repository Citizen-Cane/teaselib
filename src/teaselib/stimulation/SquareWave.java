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
        add(MAX, onTimeMillis);
        add(MIN, offTimeMillis);
    }

    public SquareWave(double onTimeSeconds, double offTimeSeconds) {
        add(MAX, toMillis(onTimeSeconds));
        add(MIN, toMillis(offTimeSeconds));
    }

    public static SquareWave get(double periodSeconds, double onPercentage) {
        double onDuration = periodSeconds * clamp(onPercentage);
        return new SquareWave(onDuration, periodSeconds - onDuration);
    }

}
