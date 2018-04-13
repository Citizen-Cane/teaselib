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
// TODO Make interface and implementation ArrayBased waveform, because arrays will be inefficient for non-rectangular
// waveforms - its just done this way for now because the XInput-Controller is happy with square waves
public class WaveForm {
    static final double MIN = 0.0;
    static final double MAX = 1.0;

    public static class Entry {
        public final double amplitude;
        public final long timeStampMillis;

        public Entry(double amplitude, long durationMillis) {
            super();
            this.amplitude = amplitude;
            this.timeStampMillis = durationMillis;
        }

        @Override
        public String toString() {
            return "[" + amplitude + "+" + timeStampMillis + "ms]";
        }
    }

    public final List<Entry> values;
    private long end;

    public WaveForm() {
        this.values = new ArrayList<>();
        this.end = 0;
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

    public long getDurationMillis() {
        return end;
    }

    // TODO Replace by iterable entry to avoid looping twice
    // keep in mind that the abstraction must also cover audio waveforms
    // - audio waveforms have constant distance between samples - will be O(1)
    public long nextTime(long currentTimeMillis) {
        for (Entry entry : values) {
            if (currentTimeMillis <= entry.timeStampMillis) {
                return entry.timeStampMillis;
            }
        }
        return Long.MAX_VALUE;
    }

    public double getValue(long timeMillis) {
        for (Entry entry : values) {
            if (timeMillis <= entry.timeStampMillis) {
                return entry.amplitude;
            }
        }
        return 0.0;
    }
}
