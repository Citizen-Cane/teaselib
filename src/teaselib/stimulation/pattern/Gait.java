package teaselib.stimulation.pattern;

import java.util.concurrent.TimeUnit;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author Citizen-Cane
 *
 */
public class Gait implements Stimulation {
    final double periodDurationSeconds;

    public Gait(double periodDurationSeconds) {
        this.periodDurationSeconds = periodDurationSeconds;
    }

    public Repeat over(long duration, TimeUnit unit) {
        return new Repeat(this, duration, unit);
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        double onTimeSeconds = Stimulation.spreadRange(Math.max(0.2, stimulator.minimalSignalDuration()), 0.5,
                intensity);
        return new SquareWave(onTimeSeconds, periodDurationSeconds - onTimeSeconds);
    }
}
