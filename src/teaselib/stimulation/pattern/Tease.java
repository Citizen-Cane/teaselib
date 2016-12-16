/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author someone Periodic bursts
 */
public class Tease extends Stimulation {

    private final double burstOnOffSeconds;

    public Tease(Stimulator stimulator) {
        this(stimulator, 1.0, 0.1);
    }

    public Tease(Stimulator stimulator, double periodDurationSeconds,
            double burstOnOffSeconds) {
        super(stimulator, periodDurationSeconds);
        this.burstOnOffSeconds = burstOnOffSeconds;
    }

    @Override
    public WaveForm waveform(int intensity) {
        double onTimeMillis = periodDurationSeconds * intensity / MaxIntensity;
        return new BurstSquareWave(periodDurationSeconds * 1000,
                onTimeMillis * 1000, burstOnOffSeconds * 1000);
    }

}
