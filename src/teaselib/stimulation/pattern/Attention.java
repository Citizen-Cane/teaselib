package teaselib.stimulation.pattern;

import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class Attention extends Stimulation {
    static final double MinOnDurationSeconds = 5.0;
    static final double IntensityFactor = 1.0;

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTimeSeconds = MinOnDurationSeconds + IntensityFactor * intensity;
        return new ConstantWave(onTimeSeconds);
    }
}
