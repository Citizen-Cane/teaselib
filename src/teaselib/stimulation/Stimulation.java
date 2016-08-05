/**
 * 
 */
package teaselib.stimulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;

/**
 * Assign a specific stimulation to a stimulator.
 * 
 * @author someone
 *
 */
public abstract class Stimulation {
    private static final Logger logger = LoggerFactory
            .getLogger(Stimulation.class);

    /**
     * The body region the stimulator is applied to
     *
     */
    public enum BodyPart {
        Anus,
        Balls,
        Buttocks,
        Cock,
        Thighs,
        Tits
    }

    public enum Type {
        Walk,
        Trot,
        Run,
        Attention,
        Whip,
        Punish,
        Tease,
        Cum
    }

    protected final static int MaxIntensity = 10;
    protected final static double maxStrength = 1.0;

    final Stimulator stimulator;

    private Thread stim = null;
    public final double periodDurationSeconds;

    public Stimulation(Stimulator stimulator, double periodDurationSeconds) {
        super();
        this.stimulator = stimulator;
        this.periodDurationSeconds = Math.max(periodDurationSeconds,
                stimulator.minimalSignalDuration());
    }

    public void play(final int intensity, final double durationSeconds) {
        // TODO Thread pool, Future
        stim = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    waveform(intensity).play(stimulator, durationSeconds,
                            Stimulation.maxStrength);
                } catch (InterruptedException e) {
                    stimulator.set(0);
                }
            }
        });
        final String simpleName = getClass().getSimpleName();
        logger.info(simpleName + ": intensity=" + intensity + " duration="
                + durationSeconds + " on " + stimulator.getDeviceName() + ", "
                + stimulator.getLocation());
        stim.setName(simpleName);
        stim.start();
    }

    public void stop() {
        stim.interrupt();
        while (stim.isAlive()) {
            try {
                stim.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }
    }

    public void complete() {
        while (stim.isAlive()) {
            try {
                stim.join();
            } catch (InterruptedException e) {
                stop();
                throw new ScriptInterruptedException();
            }
        }
    }

    protected abstract WaveForm waveform(int intensity);
}
