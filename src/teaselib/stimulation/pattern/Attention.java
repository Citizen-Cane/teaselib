package teaselib.stimulation.pattern;

import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class Attention implements Stimulation {
    static final double MinOnDurationSeconds = 5.0;
    static final double IntensityFactor = 1.0;

    private final double durationSeconds;

    public Attention() {
        this.durationSeconds = MinOnDurationSeconds;
    }

    public Attention(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTimeSeconds = durationSeconds + IntensityFactor * intensity;
        return new ConstantWave(onTimeSeconds);
    }
}
