/**
 * 
 */
package teaselib.stimulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for stimulations. Links meaning of a stimulation to a specific stimulator.
 * 
 * @author Citizen-Cane
 *
 */
public abstract class Stimulation {
    private static final Logger logger = LoggerFactory.getLogger(Stimulation.class);

    protected static final int MinIntensity = 0;
    protected static final int MaxIntensity = 10;

    protected static final double maxStrength = 1.0;

    protected Stimulator stimulator;
    int priority;

    public class Priority {
        public static final int OneShotShort = 300;
        public static final int OneShot = 200;
        public static final int Normal = 100;
    }

    public Stimulation(Stimulator stimulator) {
        this(stimulator, Priority.Normal);
    }

    public Stimulation(Stimulator stimulator, int priority) {
        super();
        this.stimulator = stimulator;
        this.priority = priority;
    }

    public void play(double durationSeconds, int intensity) {
        if (logger.isInfoEnabled()) {
            logger.info(getClass().getSimpleName() + ": intensity=" + intensity + " duration=" + durationSeconds
                    + " on " + stimulator.getDeviceName() + ", " + stimulator.getLocation());
        }
        stimulator.play(waveform(intensity), durationSeconds, Stimulation.maxStrength);
    }

    public void extend(double durationSeconds) {
        stimulator.extend(durationSeconds);
    }

    public void stop() {
        stimulator.stop();
    }

    public void complete() {
        stimulator.complete();
    }

    protected abstract WaveForm waveform(int intensity);

    public static double spreadRange(double from, double to, int intensity) {
        return from + (to - from) * intensity / MaxIntensity;
    }
}
