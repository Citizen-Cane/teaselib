package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import teaselib.stimulation.WaveForm;

public class StimulationChannels implements Iterable<Channel> {
    final List<Channel> channels;

    class Sample {
        long timeStampMillis;
        final double[] values;

        public Sample(int channels) {
            timeStampMillis = Long.MIN_VALUE;
            values = new double[channels];
        }

    }

    class IteratorImpl implements Iterator<Sample> {
        final Sample sample;

        final List<Iterator<WaveForm.Sample>> pointers;
        final List<WaveForm.Sample> samples;

        long timeStampMillis = 0;

        private IteratorImpl() {
            this.sample = new Sample(size());
            this.samples = new ArrayList<>(size());
            this.pointers = new ArrayList<>(size());

            for (Channel channel : channels) {
                Iterator<WaveForm.Sample> iterator = channel.getWaveForm().iterator();
                pointers.add(iterator);
                samples.add(iterator.next());
            }
        }

        @Override
        public boolean hasNext() {
            return pointers.stream().filter(Iterator::hasNext).count() > 0;
        }

        @Override
        public Sample next() {
            throw new UnsupportedOperationException();
            // TODO advancing through multiple waveform samples requires to remember time for each sample between sample
            // boundaries
        }
    }

    public class SampleIterator implements Iterator<double[]> {
        final double[] values;
        long timeStampMillis;

        private SampleIterator() {
            values = new double[channels.size()];
            timeStampMillis = Long.MIN_VALUE;
        }

        public long getTimeStampMillis() {
            return timeStampMillis;
        }

        @Override
        public boolean hasNext() {
            return channels.stream().map(Channel::getWaveForm).map(WaveForm::getDurationMillis).reduce(Math::max)
                    .orElseGet(() -> Long.MIN_VALUE) > timeStampMillis;
        }

        @Override
        public double[] next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                timeStampMillis = nextTimeStamp(timeStampMillis);
                getSamples(timeStampMillis, values);
                return values;
            }
        }

        private long nextTimeStamp(long currentTimeMillis) {
            Optional<Long> min = channels.stream().map(Channel::getWaveForm)
                    .map((WaveForm waveform) -> waveform.nextTime(currentTimeMillis)).reduce(Math::min);
            if (min.isPresent()) {
                return min.get();
            } else {
                throw new IllegalArgumentException(channels.toString());
            }
        }

        private void getSamples(long timeStampMillis, double[] values) {
            for (int i = 0; i < values.length; i++) {
                values[i] = channels.get(i).getValue(timeStampMillis);
            }
        }

        @Override
        public String toString() {
            return timeStampMillis + "ms -> " + Arrays.toString(values);
        }
    }

    public StimulationChannels() {
        this.channels = new ArrayList<>();
    }

    public StimulationChannels(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public Iterator<Channel> iterator() {
        return channels.iterator();
    }

    @Override
    public void forEach(Consumer<? super Channel> action) {
        channels.forEach(action);
    }

    @Override
    public Spliterator<Channel> spliterator() {
        return channels.spliterator();
    }

    public SampleIterator sampleIterator() {
        return new SampleIterator();
    }

    public long maxDurationMillis() {
        Optional<Channel> max = channels.stream().reduce(Channel::maxDuration);
        if (max.isPresent()) {
            return max.get().waveForm.getDurationMillis();
        } else {
            throw new IllegalStateException();
        }
    }

    public int size() {
        return channels.size();
    }

    public Stream<Channel> stream() {
        return channels.stream();
    }

    public Channel get(int index) {
        return channels.get(index);
    }

    public boolean isEmpty() {
        return channels.isEmpty();
    }

    public void add(Channel channel) {
        channels.add(channel);
    }

    @Override
    public String toString() {
        return channels.toString();
    }

}
