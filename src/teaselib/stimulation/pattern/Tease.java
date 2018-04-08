package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * Periodic bursts up to 25 of the period duration. Meant to distract - longer periods work great with stronger signals.
 * Stimulations works best when turned on/off regularly.
 * 
 * @author Citizen-Cane
 */
public class Tease extends Stimulation {
    static final double DurationSeconds = 2.0;

    private final double periodDurationSeconds;

    public Tease(Stimulator stimulator) {
        this(stimulator, DurationSeconds);
    }

    public Tease(Stimulator stimulator, double periodDuration) {
        super(stimulator);
        this.periodDurationSeconds = periodDuration;
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTime = spreadRange(Math.max(0.1, stimulator.minimalSignalDuration()), periodDurationSeconds * 0.25,
                intensity);
        return new SquareWave(onTime, periodDurationSeconds - onTime);
    }
}
