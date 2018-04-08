package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class Gait extends Stimulation {
    final double periodDurationSeconds;

    public Gait(Stimulator stimulator, double periodDurationSeconds) {
        super(stimulator);
        this.periodDurationSeconds = periodDurationSeconds;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTimeSeconds = spreadRange(Math.max(0.2, stimulator.minimalSignalDuration()), 0.5, intensity);
        return new SquareWave(onTimeSeconds, periodDurationSeconds - onTimeSeconds);
    }
}
