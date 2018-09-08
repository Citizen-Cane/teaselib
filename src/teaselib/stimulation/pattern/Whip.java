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
    static final double DEFAULT_PERIOD_SECONDS = 1.0;

    private final int strokes;
    private final double periodDurationSeconds;

    public Whip() {
        this(1);
    }

    public Whip(int strokes) {
        this(strokes, DEFAULT_PERIOD_SECONDS);
    }

    public Whip(int strokes, double periodDurationSeconds) {
        if (strokes <= 0) {
            throw new IllegalArgumentException("Strokes must be greater than 0");
        }
        if (periodDurationSeconds <= 0) {
            throw new IllegalArgumentException("Period seconds must be greater than 0");
        }
        this.strokes = strokes;
        this.periodDurationSeconds = periodDurationSeconds;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double on = stimulator.minimalSignalDuration() * Stimulation.spreadRange(1.0, 2.0, intensity);
        return new BurstSquareWave(strokes, //
                Math.min(on, periodDurationSeconds), //
                Math.max(0, this.periodDurationSeconds - on));
    }
}
