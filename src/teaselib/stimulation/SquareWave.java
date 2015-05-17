/**
 * 
 */
package teaselib.stimulation;

/**
 * @author someone
 *
 */
public class SquareWave extends WaveForm {
    final long periodTimeMillis;
    final long onTimeMillis;

    /**
     * @param periodTimeMillis
     *            Duration of a period
     * @param onTimeMillis
     *            The on-time during each period
     */
    public SquareWave(long periodTimeMillis, long onTimeMillis) {
        this.periodTimeMillis = periodTimeMillis;
        this.onTimeMillis = onTimeMillis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * teaselib.stimulation.StimulationPattern#play(teaselib.stimulation.Stimulator
     * , int, double)
     */
    @Override
    public void play(Stimulator stimumlator, double seconds, double strength) {
        long startTime = System.currentTimeMillis();
        long durationMillis = (long) seconds * 1000;
        try {
            do {
                stimumlator.set(strength);
                Thread.sleep(onTimeMillis);
                stimumlator.set(0.0);
                Thread.sleep(periodTimeMillis - onTimeMillis);
            } while (System.currentTimeMillis() - startTime < durationMillis);
        } catch (InterruptedException e) {
            // Stop
        }
    }
}
