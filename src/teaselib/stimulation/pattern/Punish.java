/**
 * 
 */
package teaselib.stimulation.pattern;

import teaselib.stimulation.BurstSquareWave;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

/**
 * @author someone
 *
 */
public class Punish extends Stimulation {

    public Punish(Stimulator stimulator) {
        super(stimulator, 0.0);
    }

    @Override
    public WaveForm waveform(int intensity) {
        double punishSeconds = Punish.getSeconds(intensity);
        // A constant signal
        return new BurstSquareWave(punishSeconds * 1000, punishSeconds * 1000,
                0.05 * 1000);
    }

    public static double getSeconds(int intensity) {
        return 2.0 * intensity / MaxIntensity;
    }
}
