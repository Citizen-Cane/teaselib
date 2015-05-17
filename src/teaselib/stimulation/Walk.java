/**
 * 
 */
package teaselib.stimulation;

/**
 * @author someone
 *
 */
public class Walk extends Stimulation {

    public static final double StepDurationSeconds = 1.4;

    public Walk(Stimulator stimulator) {
        super(stimulator);
    }

    @Override
    public void run() {
        double d = StepDurationSeconds * 1000;
        long onTimeMillis = (long) ((d * intensity / maxIntensity) * 0.6);
        new SquareWave((long) d, onTimeMillis).play(stimulator,
                durationSeconds, Stimulation.maxStrength);
    }
}
