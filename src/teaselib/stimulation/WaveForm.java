package teaselib.stimulation;

import java.util.ArrayList;
import java.util.Iterator;
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
public class WaveForm implements Iterable<WaveForm.Sample> {

    public static final double MIN = 0.0;
    public static final double MAX = 1.0;
    public static final double MEAN = (MAX - MIN) / 2.0;

    public static final WaveForm NONE = WaveForm.empty();

    public static class Entry {
        public final double amplitude;
        public final long durationMillis;

        public Entry(double amplitude, long durationMillis) {
            if (durationMillis < 0)
                throw new IllegalArgumentException("Duration must be positive: " + durationMillis);
            this.amplitude = amplitude;
            this.durationMillis = durationMillis;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(amplitude);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + (int) (durationMillis ^ (durationMillis >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Entry other = (Entry) obj;
            if (Double.doubleToLongBits(amplitude) != Double.doubleToLongBits(other.amplitude))
                return false;
            if (durationMillis != other.durationMillis)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[" + amplitude + "+" + durationMillis + "ms]";
        }
    }

    public final List<Entry> values;
    private long end;

    public WaveForm() {
        this.values = new ArrayList<>();
        this.end = 0;
    }

    private static WaveForm empty() {
        WaveForm empty = new WaveForm();
        empty.add(0, 0);
        return empty;
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
        end += entry.durationMillis;
    }

    @Override
    public String toString() {
        return values.toString() + "->" + end;
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

    @Override
    public Iterator<Sample> iterator() {
        return new IteratorImpl();
    }

    public static class Sample {
        long timeStampMillis;
        double value;

        public static final Sample End = new Sample(Long.MAX_VALUE, 0.0);

        private Sample() {
            this(Long.MIN_VALUE, 0.0);
        }

        public Sample(long timeStampMillis, double value) {
            this.timeStampMillis = timeStampMillis;
            this.value = value;
        }

        public long getTimeStampMillis() {
            return timeStampMillis;
        }

        public double getValue() {
            return value;
        }

        public static Sample earliest(Sample a, Sample b) {
            return a.timeStampMillis < b.timeStampMillis ? a : b;
        }

        @Override
        public String toString() {
            return timeStampMillis + "ms -> " + value;
        }
    }

    class IteratorImpl implements Iterator<WaveForm.Sample> {
        final Sample sample = new Sample();
        final Iterator<Entry> entry = values.iterator();
        long timeStampMillis = 0;

        @Override
        public boolean hasNext() {
            return entry.hasNext();
        }

        @Override
        public Sample next() {
            Entry current = entry.next();
            sample.timeStampMillis = timeStampMillis;
            sample.value = current.amplitude;
            timeStampMillis += current.durationMillis;
            return sample;
        }
    }

    public long nextTime(long currentTimeMillis) {
        long timeStampMillis = 0;
        for (Entry entry : values) {
            if (currentTimeMillis < timeStampMillis) {
                return timeStampMillis;
            }
            timeStampMillis += entry.durationMillis;
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
            if (timeMillis < timeStampMillis + entry.durationMillis) {
                return entry.amplitude;
            }
            timeStampMillis += entry.durationMillis;
        }
        return 0.0;
    }

    public WaveForm slice(long startMillis) {
        return slice(startMillis, getDurationMillis());
    }

    public WaveForm slice(long startMillis, long endMillis) {
        WaveForm slice = new WaveForm();
        Iterator<Entry> iterator = values.iterator();
        long timeStampMillis = 0;

        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            timeStampMillis += entry.durationMillis;
            if (timeStampMillis > startMillis) {
                slice.add(new Entry(entry.amplitude, timeStampMillis - startMillis));
                break;
            }
        }

        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (timeStampMillis + entry.durationMillis <= endMillis) {
                slice.add(entry);
            } else {
                if (timeStampMillis < endMillis) {
                    slice.add(new Entry(entry.amplitude, endMillis - timeStampMillis));
                }
                break;
            }
            timeStampMillis += entry.durationMillis;
        }

        return slice;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (end ^ (end >>> 32));
        result = prime * result + ((values == null) ? 0 : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WaveForm other = (WaveForm) obj;
        if (end != other.end)
            return false;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
