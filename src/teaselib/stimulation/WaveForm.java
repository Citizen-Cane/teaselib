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

    abstract public void play(Stimulator stim, double seconds, double strength)
            throws InterruptedException;

    public void extend(double additonalSeconds) {
        durationMillis.addAndGet((long) (additonalSeconds * 1000));
    }
}
