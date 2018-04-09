package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Channel {
    public final Stimulator stimulator;
    public final WaveForm waveForm;

    public Channel(Stimulator stimulator, WaveForm waveForm) {
        this.stimulator = stimulator;
        this.waveForm = waveForm;
    }

    public static Channel maxDuration(Channel a, Channel b) {
        return a.waveForm.getDuration() > b.waveForm.getDuration() ? a : b;
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }
}