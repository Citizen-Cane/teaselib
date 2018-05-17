package teaselib.stimulation;

/**
 * Base class for stimulations. Links meaning of a stimulation to a specific stimulator.
 * 
 * @author Citizen-Cane
 *
 */
@FunctionalInterface
public interface Stimulation {
    public static final int MinIntensity = 0;
    public static final int MaxIntensity = 10;

    public static final double MaxStrength = 1.0;

    WaveForm waveform(Stimulator stimulator, int intensity);

    public static double spreadRange(double from, double to, int intensity) {
        return from + (to - from) * intensity / MaxIntensity;
    }
}
