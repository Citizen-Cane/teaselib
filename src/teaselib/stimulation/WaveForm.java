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
// TODO Turn into interface and ArrayListBased waveform implementation, because arrays will be inefficient for
// non-rectangular waveforms - this is closely tied to the XInput-Controller since that one is happy with square waves
public class WaveForm {
    public static final double MIN = 0.0;
    public static final double MAX = 1.0;
    public static final double MEAN = (MAX - MIN) / 2.0;

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

    public WaveForm(long startMillis, WaveForm waveForm) {
        this();
        add(0.0, startMillis);
        for (Entry entry : waveForm.values) {
            add(entry);
        }
    }

    public void add(double amplitude, long timeStampMillis) {
        add(new Entry(amplitude, timeStampMillis));
    }

    private void add(Entry entry) {
        this.values.add(entry);
        end += entry.timeStampMillis;
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
        long timeStampMillis = 0;
        for (Entry entry : values) {
            if (currentTimeMillis < timeStampMillis) {
                return timeStampMillis;
            }
            timeStampMillis += entry.timeStampMillis;
        }
        if (timeStampMillis <= end && currentTimeMillis < end) {
            return timeStampMillis;
        } else {
            return Long.MAX_VALUE;
        }
    }

    public double getValue(long timeMillis) {
        if (timeMillis < 0) {
            return 0.0;
        } else {
            return value(timeMillis);
        }
    }

    private double value(long timeMillis) {
        long timeStampMillis = 0;
        for (Entry entry : values) {
            if (timeMillis <= timeStampMillis) {
                return entry.amplitude;
            }
            timeStampMillis += entry.timeStampMillis;
        }
        return 0.0;
    }
}
