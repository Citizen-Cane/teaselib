/**
 * 
 */
package teaselib.stimulation;

import java.util.Arrays;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class WaveForm {
    static final double MIN = 0.0;
    static final double MAX = 1.0;

    public static class Entry {
        public final double amplitude;
        public final long durationMillis;

        public Entry(double amplitude, long durationMillis) {
            super();
            this.amplitude = amplitude;
            this.durationMillis = durationMillis;
        }
    }

    public final List<Entry> values;

    public WaveForm(Entry... values) {
        this.values = Arrays.asList(values);
    }

    public void add(Entry... entries) {
        for (Entry entry : entries) {
            values.add(entry);
        }
    }

    static long toMillis(double seconds) {
        return (int) (seconds * 1000);
    }

    public static double clamp(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }
}
