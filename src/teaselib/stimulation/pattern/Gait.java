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

    final double onTimeBaseSeconds;

    public Gait(Stimulator stimulator, double periodDurationSeconds,
            double onTimeBaseSeconds) {
        super(stimulator, periodDurationSeconds);
        this.onTimeBaseSeconds = onTimeBaseSeconds;
    }

    @Override
    public void play() throws InterruptedException {
        double onTimeMillis = onTimeBaseSeconds + periodDurationSeconds
                * intensity / MaxIntensity * 0.4;
        new SquareWave(periodDurationSeconds * 1000, onTimeMillis * 1000).play(
                stimulator, durationSeconds, Stimulation.maxStrength);
    }
}
