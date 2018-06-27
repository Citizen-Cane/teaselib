package teaselib.stimulation;

/**
 * Base class for stimulations. Links meaning of a stimulation to a specific stimulator.
 * 
 * @author Citizen-Cane
 *
 */
@FunctionalInterface
public interface Stimulation {
    static final int MinIntensity = 0;
    static final int MaxIntensity = 10;
    static final double MaxStrength = 1.0;
    static final Stimulation NONE = (stimulator1, intensity) -> WaveForm.NONE;

    WaveForm waveform(Stimulator stimulator, int intensity);

    public static double spreadRange(double from, double to, int intensity) {
        return from + (to - from) * intensity / MaxIntensity;
    }
}
