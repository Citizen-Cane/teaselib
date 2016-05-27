/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author someone
 *
 */
public class Wave extends Stimulation {
    private final WaveForm waveForm;

    public Wave(Stimulator stimulator, WaveForm waveForm,
            double periodDurationSeconds) {
        super(stimulator, periodDurationSeconds);
        this.waveForm = waveForm;
    }

    @Override
    protected WaveForm waveform(int intensity) {
        return waveForm;
    }
}
