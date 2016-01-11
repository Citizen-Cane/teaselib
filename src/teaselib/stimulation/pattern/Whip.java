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

    /**
     * @param stimulator
     * @param baseDuration
     *            A longer base duration works better for anal stimulation
     */
    public Whip(Stimulator stimulator, double baseDuration) {
        super(stimulator, baseDuration);
    }

    @Override
    public void play() throws InterruptedException {
        // Attention duration depends on intensity only
        double whipSeconds = getSeconds(intensity);
        // A constant signal
        new SquareWave(whipSeconds * 1000.0, whipSeconds * 1000.0).play(
                stimulator, durationSeconds, Stimulation.maxStrength);
    }

    public double getSeconds(int intensity) {
        return periodDurationSeconds + 0.5 * intensity / MaxIntensity;
    }

}
