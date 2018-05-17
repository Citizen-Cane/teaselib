package teaselib.stimulation.pattern;

import java.util.concurrent.TimeUnit;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Repeat implements Stimulation {
    private final Stimulation stimulation;
    private final long durationMillis;

    public Repeat(Stimulation stimulation, long duration, TimeUnit unit) {
        this.stimulation = stimulation;
        this.durationMillis = unit.toMillis(duration);
    }

    @Override
    public WaveForm waveform(Stimulator stimulator, int intensity) {
        WaveForm all = new WaveForm();
        WaveForm repeated = stimulation.waveform(stimulator, intensity);
        long elapsed = 0;
        while (elapsed < durationMillis) {
            all.values.addAll(repeated.values);
            elapsed += repeated.getDurationMillis();
        }
        return all;
    }
}
