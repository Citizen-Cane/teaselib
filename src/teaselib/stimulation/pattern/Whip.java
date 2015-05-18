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
public class Whip extends Stimulation {

    public Whip(Stimulator stimulator) {
        super(stimulator, 0.0);
    }

    @Override
    public void run() {
        // Attention duration depends on intensity only
        double whipSeconds = Whip.getSeconds(intensity);
        // A constant signal
        new SquareWave(whipSeconds * 1000, whipSeconds * 1000).play(stimulator,
                durationSeconds, Stimulation.maxStrength);
    }

    public static double getSeconds(int intensity) {
        return 0.0 + 0.5 * intensity / MaxIntensity;
    }

}
