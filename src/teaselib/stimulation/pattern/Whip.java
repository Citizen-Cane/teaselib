package teaselib.stimulation.pattern;

import teaselib.stimulation.ConstantWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * The idea of the whip is that the sub adjusts the signal strength to the max bearable amount. Then during the session
 * the intensity is slightly increased in order to compensate for decreaesed sensibility.
 * <p>
 * Intensity is increased just a little, since the whip is mainly for control, in opposite to the dedicated punishment
 * stimulations.
 * 
 * @author Citizen-Cane
 *
 */
public class Whip extends Stimulation {

    public Whip(Stimulator stimulator) {
        super(stimulator);
    }

    @Override
    public WaveForm waveform(int intensity) {
        return new ConstantWave(stimulator.minimalSignalDuration() * spreadRange(1.0, 2.0, intensity));
    }
}
