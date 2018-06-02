package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class StimulationTarget {
    static final StimulationTarget EMPTY = new StimulationTarget(null, WaveForm.NONE, 0) {
        @Override
        public String toString() {
            return "Empty";
        }
    };

    final Stimulator stimulator;
    final WaveForm waveForm;
    final int repeatCount;

    public StimulationTarget(Stimulator stimulator, WaveForm waveForm) {
        this(stimulator, waveForm, 0, 1);
    }

    public StimulationTarget(Stimulator stimulator, WaveForm waveForm, long startMillis) {
        this(stimulator, waveForm, startMillis, 1);
    }

    public StimulationTarget(Stimulator stimulator, WaveForm waveForm, long startMillis, long durationMillis) {
        this(stimulator, waveForm, startMillis, repeatCount(waveForm, durationMillis));
    }

    private StimulationTarget(Stimulator stimulator, WaveForm waveForm, long startMillis, int repeatCount) {
        this.stimulator = stimulator;
        this.waveForm = startMillis > 0 ? new WaveForm(startMillis, waveForm) : waveForm;
        this.repeatCount = repeatCount;
    }

    private static int repeatCount(WaveForm waveForm, long durationMillis) {
        return durationMillis > 0 ? Math.max(1, (int) (durationMillis / waveForm.getDurationMillis())) : 1;
    }

    public WaveForm getWaveForm() {
        return waveForm;
    }

    public static StimulationTarget maxDuration(StimulationTarget a, StimulationTarget b) {
        return a.waveForm.getDurationMillis() > b.waveForm.getDurationMillis() ? a : b;
    }

    public double getValue(long timeStampMillis) {
        return waveForm.getValue(timeStampMillis);
    }

    @Override
    public String toString() {
        return stimulator + "->" + waveForm;
    }

    public StimulationTarget delayed(long startMillis) {
        return startMillis > 0 ? new StimulationTarget(stimulator, waveForm, startMillis, repeatCount) : this;
    }

    public StimulationTarget slice(long startMillis) {
        return new StimulationTarget(stimulator, waveForm.slice(startMillis));
    }
}