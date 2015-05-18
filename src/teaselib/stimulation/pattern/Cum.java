/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

/**
 * @author someone
 *
 */
public class Cum extends Stimulation {

    public Cum(Stimulator stimulator) {
        super(stimulator, 1.0);
    }

    @Override
    public void run() {
        double onTimeMillis = periodDurationSeconds * 0.9;
        double burstBase = 0.3;
        double onFactor = (double) intensity
                / (double) Stimulation.MaxIntensity;
        double burstOnSeconds = burstBase * onFactor;
        double burstOffSeconds = burstBase * (1.0 - onFactor);
        new BurstSquareWave(periodDurationSeconds * 1000, onTimeMillis * 1000,
                burstOnSeconds * 1000, burstOffSeconds * 1000).play(stimulator,
                durationSeconds, Stimulation.maxStrength);
    }

}
