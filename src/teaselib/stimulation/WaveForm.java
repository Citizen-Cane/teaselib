package teaselib.stimulation;

import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of values with absolute time stamps. Entries contain durations, internally the entries are stored with
 * absolute time stamps.
 * 
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

        @Override
        public String toString() {
            return "[" + amplitude + "+" + durationMillis + "ms]";
        }
    }

    public final List<Entry> values;

    private long start;
    private long end;

    public WaveForm() {
        this(0);
    }

    public WaveForm(long start) {
        this.values = new ArrayList<>();
        this.start = start;
        this.end = start;
    }

    public void add(double amplitude, long durationMillis) {
        this.values.add(new Entry(amplitude, end));
        end += durationMillis;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public static long toMillis(double seconds) {
        return (long) (seconds * 1000);
    }

    public static double clamp(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }

    public int size() {
        return values.size();
    }

    public long getDuration() {
        return end - start;
    }
}
