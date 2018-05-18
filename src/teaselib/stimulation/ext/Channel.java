package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class Channel {
    static final Channel EMPTY = new Channel(null, WaveForm.NONE) {
        @Override
        public String toString() {
            return "Empty";
        }
    };

    final Stimulator stimulator;
    final WaveForm waveForm;

    public Channel(Stimulator stimulator, WaveForm waveForm, long startMillis) {
        this(stimulator, startMillis > 0 ? new WaveForm(startMillis, waveForm) : waveForm);
    }

    public Channel(Stimulator stimulator, WaveForm waveForm) {
        this.stimulator = stimulator;
        this.waveForm = waveForm;
    }

    public WaveForm getWaveForm() {
        return waveForm;
    }

    public static Channel maxDuration(Channel a, Channel b) {
        return a.waveForm.getDurationMillis() > b.waveForm.getDurationMillis() ? a : b;
    }

    public double getValue(long timeStampMillis) {
        return waveForm.getValue(timeStampMillis);
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }
}