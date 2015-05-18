/**
 * 
 */
package teaselib.stimulation;

/**
 * @author someone
 *
 */
public class BurstSquareWave extends WaveForm {
    final long periodTimeMillis;
    final long onTimeMillis;
    final long burstOnMillis;
    final long burstOffMillis;

    /**
     * @param periodTimeMillis
     *            Duration of a period
     * @param onTimeMillis
     *            The on-time during each period
     */
    public BurstSquareWave(double periodTimeMillis, double onTimeMillis,
            double burstOnOffMillis) {
        this.periodTimeMillis = (long) periodTimeMillis;
        this.onTimeMillis = (long) onTimeMillis;
        this.burstOnMillis = (long) burstOnOffMillis;
        this.burstOffMillis = (long) burstOnOffMillis;
    }

    public BurstSquareWave(double periodTimeMillis, double onTimeMillis,
            double burstOnMillis, double burstOffMillis) {
        this.periodTimeMillis = (long) periodTimeMillis;
        this.onTimeMillis = (long) onTimeMillis;
        this.burstOnMillis = (long) burstOnMillis;
        this.burstOffMillis = (long) burstOffMillis;
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
                long t = 0;
                while (t <= onTimeMillis) {
                    stimumlator.set(strength);
                    Thread.sleep(burstOnMillis);
                    t += burstOnMillis;
                    stimumlator.set(0.0);
                    Thread.sleep(burstOffMillis);
                    t += burstOffMillis;
                }
                if (t < periodTimeMillis) {
                    Thread.sleep(periodTimeMillis - t);
                }
            } while (System.currentTimeMillis() - startTime < durationMillis);
        } catch (InterruptedException e) {
            // Stop
        }
    }
}
