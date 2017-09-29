package teaselib.stimulation;

/**
 * @author Citizen-Cane
 *
 */
public class BurstSquareWave extends WaveForm {
    public BurstSquareWave(int n, double onTimeSeconds, double offTimeSeconds) {
        super();

        for (int i = 0; i < n; i++) {
            add(new Entry(MAX, toMillis(onTimeSeconds)), new Entry(MIN, toMillis(offTimeSeconds)));
        }
    }
}
