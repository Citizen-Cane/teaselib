/**
 * 
 */
package teaselib.stimulation;

/**
 * @author someone
 *
 */
public abstract class Stimulation implements Runnable {

    protected final static int MaxIntensity = 10;
    protected final static double maxStrength = 1.0;

    protected final Stimulator stimulator;
    protected int intensity;

    // todo prevent change from thread
    protected double durationSeconds;

    private Thread stim = null;
    public final double periodDurationSeconds;

    public Stimulation(Stimulator stimulator, double periodDurationSeconds) {
        super();
        this.stimulator = stimulator;
        this.periodDurationSeconds = periodDurationSeconds;
    }

    public void play(int intensity, double durationSeconds) {
        this.intensity = intensity;
        this.durationSeconds = durationSeconds;
        stim = new Thread(this);
        stim.start();
    }

    public void extend(double additionalSeconds) {
        // todo lock or atomic
        durationSeconds += additionalSeconds;
    }

    public void stop() {
        stim.interrupt();
        stim = null;
    }

    @Override
    public abstract void run();
}
