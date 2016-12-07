/**
 * 
 */
package teaselib.stimulation;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author someone
 *
 */
public abstract class WaveForm {

    final AtomicLong durationMillis = new AtomicLong();

    /**
     * Play a waveform for the given duration on the provided stimulator with
     * the given strength.
     * 
     * @param stim
     *            The stimulator to play the waveform on.
     * @param seconds
     *            The time to play the waveform.
     * @param strength
     *            The strength the waveform is played with.
     * @throws InterruptedException
     */
    abstract public void play(Stimulator stim, double seconds, double strength)
            throws InterruptedException;

    public void extend(double additonalSeconds) {
        durationMillis.addAndGet((long) (additonalSeconds * 1000));
    }
}
