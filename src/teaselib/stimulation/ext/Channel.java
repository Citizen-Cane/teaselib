package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Channel {
    final Stimulator stimulator;
    final WaveForm waveForm;

    public Channel(Stimulator stimulator, WaveForm waveForm) {
        this.stimulator = stimulator;
        this.waveForm = waveForm;
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }
}