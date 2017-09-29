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
        super(new Entry(MAX, onTimeMillis), new Entry(MIN, offTimeMillis));
    }

    public SquareWave(double onTimeSeconds, double offTimeSeconds) {
        super(new Entry(MAX, toMillis(onTimeSeconds)), new Entry(MIN, toMillis(offTimeSeconds)));
    }

    public static SquareWave get(double periodSeconds, double onPercentage) {
        double onDuration = periodSeconds * clamp(onPercentage);
        return new SquareWave(onDuration, periodSeconds - onDuration);
    }

}
