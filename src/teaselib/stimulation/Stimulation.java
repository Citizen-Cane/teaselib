/**
 * 
 */
package teaselib.stimulation;

import teaselib.TeaseLib;

/**
 * @author someone
 *
 */
public abstract class Stimulation implements Runnable {
    /**
     * The body region the stimulator is applied to
     *
     */
    enum BodyPart {
        Anus, Balls, Buttocks, Cock, Thighs, Tits
    }

    public enum Type {
        Walk, Trot, Run, Attention, Whip, Punish, Tease, Cum
    }

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
        TeaseLib.log(getClass().getSimpleName() + ": intensity=" + intensity
                + " duration=" + durationSeconds);
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
