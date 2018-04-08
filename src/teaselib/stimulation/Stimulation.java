package teaselib.stimulation;

/**
 * Base class for stimulations. Links meaning of a stimulation to a specific stimulator.
 * 
 * @author Citizen-Cane
 *
 */
public abstract class Stimulation {
    public static final int MinIntensity = 0;
    public static final int MaxIntensity = 10;

    protected static final double maxStrength = 1.0;

    int priority;

    public class Priority {
        public static final int OneShotShort = 300;
        public static final int OneShot = 200;
        public static final int Normal = 100;
    }

    public Stimulation() {
        this(Priority.Normal);
    }

    public Stimulation(int priority) {
        this.priority = priority;
    }

    public WaveForm getWaveform(Stimulator stimulator, int intensity) {
        return waveform(stimulator, intensity);
    }

    protected abstract WaveForm waveform(Stimulator stimulator, int intensity);

    public static double spreadRange(double from, double to, int intensity) {
        return from + (to - from) * intensity / MaxIntensity;
    }
}
