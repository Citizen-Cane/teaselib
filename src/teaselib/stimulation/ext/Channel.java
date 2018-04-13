package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Channel {
    public final Stimulator stimulator;
    public final WaveForm waveForm;
    public final long startMillis;

    public Channel(Stimulator stimulator, WaveForm waveForm, long startMillis) {
        this.stimulator = stimulator;
        this.waveForm = waveForm;
        this.startMillis = startMillis;
    }

    public WaveForm getWaveForm() {
        return waveForm;
    }

    public static Channel maxDuration(Channel a, Channel b) {
        return a.waveForm.getDurationMillis() > b.waveForm.getDurationMillis() ? a : b;
    }

    public double getValue(long timeStampMillis) {
        return waveForm.getValue(startMillis + timeStampMillis);
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }
}