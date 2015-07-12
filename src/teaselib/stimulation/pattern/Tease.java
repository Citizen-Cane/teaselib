/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

/**
 * @author someone Periodic bursts
 */
public class Tease extends Stimulation {

    private final double burstOnOffSeconds;

    public Tease(Stimulator stimulator) {
        this(stimulator, 1.0, 0.05);
    }

    public Tease(Stimulator stimulator, double periodDurationSeconds,
            double burstOnOffSeconds) {
        super(stimulator, periodDurationSeconds);
        this.burstOnOffSeconds = burstOnOffSeconds;
    }

    @Override
    public void play() throws InterruptedException {
        double onTimeMillis = periodDurationSeconds * intensity / MaxIntensity;
        new BurstSquareWave(periodDurationSeconds * 1000, onTimeMillis * 1000,
                burstOnOffSeconds * 1000).play(stimulator, durationSeconds,
                Stimulation.maxStrength);
    }

}
