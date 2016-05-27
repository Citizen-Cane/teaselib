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
    public SquareWave(double periodTimeMillis, double onTimeMillis) {
        this.periodTimeMillis = (long) periodTimeMillis;
        this.onTimeMillis = (long) onTimeMillis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.stimulation.StimulationPattern#play(teaselib.stimulation.
     * Stimulator , int, double)
     */
    @Override
    public void play(Stimulator stimumlator, double durationSeconds,
            double strength) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        durationMillis.set((long) (durationSeconds * 1000));
        do {
            stimumlator.set(strength);
            Thread.sleep(onTimeMillis);
            stimumlator.set(0.0);
            Thread.sleep(periodTimeMillis - onTimeMillis);
        } while (System.currentTimeMillis() - startTime < durationMillis.get());
    }
}
