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
    public WaveForm waveform(int intensity) {
        double whipSeconds = getSeconds(intensity);
        // A constant signal
        return new SquareWave(whipSeconds * 1000.0, whipSeconds * 1000.0);
    }

    private double getSeconds(int intensity) {
        return periodDurationSeconds + 0.5 * intensity / MaxIntensity;
    }

}
