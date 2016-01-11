/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

/**
 * @author someone
 *
 */
public class Attention extends Stimulation {

    public Attention(Stimulator stimulator) {
        super(stimulator, 0.0);
    }

    @Override
    public void play() throws InterruptedException {
        // Attention duration depends on intensity only
        double attentionSeconds = Attention.getSeconds(intensity);
        // A constant signal
        new SquareWave(attentionSeconds * 1000, attentionSeconds * 1000).play(
                stimulator, durationSeconds, Stimulation.maxStrength);
    }

    public static double getSeconds(int intensity) {
        return 2.0 + 5.0 * intensity / MaxIntensity;
    }
}
