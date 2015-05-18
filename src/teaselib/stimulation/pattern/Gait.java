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

    public Gait(Stimulator stimulator, double periodDurationSeconds) {
        super(stimulator, periodDurationSeconds);
    }

    @Override
    public void run() {
        double onTimeMillis = periodDurationSeconds * intensity / MaxIntensity
                * 0.5;
        new SquareWave(periodDurationSeconds * 1000, onTimeMillis * 1000).play(
                stimulator, durationSeconds, Stimulation.maxStrength);
    }
}
