package teaselib.stimulation;

/**
 * @author Citizen-Cane
 *
 */
public class BurstSquareWave extends WaveForm {
    public BurstSquareWave(int n, double onTimeSeconds, double offTimeSeconds) {
        for (int i = 0; i < n; i++) {
            add(MAX, toMillis(onTimeSeconds));
            add(MIN, toMillis(offTimeSeconds));
        }
    }
}
