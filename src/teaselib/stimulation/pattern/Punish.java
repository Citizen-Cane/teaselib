/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

/**
 * @author someone
 *
 */
public class Punish extends Stimulation {

    public Punish(Stimulator stimulator) {
        super(stimulator, 0.0);
    }

    @Override
    public void play() throws InterruptedException {
        // Attention duration depends on intensity only
        double punishSeconds = Punish.getSeconds(intensity);
        // A constant signal
        new BurstSquareWave(punishSeconds * 1000, punishSeconds * 1000,
                0.05 * 1000).play(stimulator, durationSeconds,
                Stimulation.maxStrength);
    }

    public static double getSeconds(int intensity) {
        return 2.0 * intensity / MaxIntensity;
    }
}
