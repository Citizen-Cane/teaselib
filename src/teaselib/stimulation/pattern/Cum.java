package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * The long and increasing on-interval guarantees a good amount of stimulation, whereas the slightly increasing off-time
 * provides room for delayed stimulation - that is the body gets stimulated by the signal being turned off.
 * 
 * @author Citizen-Cane
 *
 */
public class Cum implements Stimulation {
    static final double MinOnDurationSeconds = 2.0;
    static final double IntensityFactor = 2.0;
    static final double OffSeconds = 2.0;

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTimeSeconds = MinOnDurationSeconds + IntensityFactor * intensity;
        double offTimeSeconds = OffSeconds * Stimulation.spreadRange(1.0, 2.0, intensity);
        return new SquareWave(onTimeSeconds, offTimeSeconds);
    }
}
