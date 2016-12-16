/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author someone
 *
 */
public class Attention extends Stimulation {

    public Attention(Stimulator stimulator) {
        super(stimulator, 0.0);
    }

    @Override
    public WaveForm waveform(int intensity) {
        // Attention duration depends on intensity only
        double attentionSeconds = Attention.getSeconds(intensity);
        // A constant signal
        return new SquareWave(attentionSeconds * 1000, attentionSeconds * 1000);
    }

    public static double getSeconds(int intensity) {
        return 2.0 + 8.0 * intensity / MaxIntensity;
    }
}
