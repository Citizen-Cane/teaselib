/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

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
    public WaveForm waveform(int intensity) {
        double onTimeMillis = onTimeBaseSeconds
                + periodDurationSeconds * intensity / MaxIntensity * 0.4;
        return new SquareWave(periodDurationSeconds * 1000,
                onTimeMillis * 1000);
    }
}
