/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

/**
 * @author someone
 *
 */
public class Gait extends Stimulation {

    public final double stepDurationSeconds;

    public Gait(Stimulator stimulator, double stepDurationSeconds) {
        super(stimulator);
        this.stepDurationSeconds = stepDurationSeconds;
    }

    @Override
    public void run() {
        double d = stepDurationSeconds * 1000;
        long onTimeMillis = (long) ((d * intensity / maxIntensity) * 0.6);
        new SquareWave((long) d, onTimeMillis).play(stimulator,
                durationSeconds, Stimulation.maxStrength);
    }
}
