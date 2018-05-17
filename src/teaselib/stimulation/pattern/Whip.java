package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
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
public class Whip implements Stimulation {
    private final int strokes;
    private final double period;

    public Whip() {
        this(1, 0.2);
    }

    public Whip(int strokes, double period) {
        this.strokes = strokes;
        this.period = period;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double on = stimulator.minimalSignalDuration() * Stimulation.spreadRange(1.0, 2.0, intensity);
        return new BurstSquareWave((int) strokes, on, this.period - on);
    }
}
