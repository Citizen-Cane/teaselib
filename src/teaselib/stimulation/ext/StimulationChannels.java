package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import teaselib.stimulation.WaveForm;
import teaselib.stimulation.WaveForm.Sample;
import teaselib.stimulation.ext.StimulationChannels.Samples;

public class StimulationChannels implements Iterable<Samples> {
    final List<Channel> channels;

    public class Samples {
        long timeStampMillis;
        final double[] values;

        public Samples(int channels) {
            timeStampMillis = Long.MIN_VALUE;
            values = new double[channels];
        }

        public long getTimeStampMillis() {
            return timeStampMillis;
        }

        public double[] getValues() {
            return values;
        }

        public double get(int index) {
            return values[index];
        }

        @Override
        public String toString() {
            return timeStampMillis + "ms -> " + Arrays.toString(values);
        }
    }

    class IteratorImpl implements Iterator<Samples> {
        final Samples samples;

        final List<Iterator<WaveForm.Sample>> iterators;
        final List<WaveForm.Sample> waveformSamples;

        long timeStampMillis = 0;

        private IteratorImpl() {
            this.samples = new Samples(size());
            this.waveformSamples = new ArrayList<>(size());
            this.iterators = new ArrayList<>(size());

            for (Channel channel : channels) {
                Iterator<WaveForm.Sample> iterator = channel.getWaveForm().iterator();
                iterators.add(iterator);
                waveformSamples.add(iterator.next());
            }
        }

        @Override
        public boolean hasNext() {
            return iterators.stream().filter(Iterator::hasNext).count() > 0;
        }

        @Override
        public Samples next() {
            Optional<Sample> sample = waveformSamples.stream().reduce(Sample::earliest);
            if (sample.isPresent() && sample.get().getTimeStampMillis() >= timeStampMillis) {
                Sample next = sample.get();
                int channel = waveformSamples.indexOf(next);
                samples.timeStampMillis = sample.get().getTimeStampMillis();
                samples.getValues()[channel] = next.getValue();
                Iterator<Sample> iterator = iterators.get(channel);
                waveformSamples.set(channel, iterator.hasNext() ? iterator.next()
                        : new WaveForm.Sample(get(channel).getWaveForm().getDurationMillis(), 0.0));
                return samples;
            } else {
                throw new IllegalStateException();
            }
        }
    }

    class SampleIterator implements Iterator<Samples> {
        Samples samples;

        private SampleIterator() {
            samples = new Samples(channels.size());
        }

        public long getTimeStampMillis() {
            return samples.timeStampMillis;
        }

        @Override
        public boolean hasNext() {
            return channels.stream().map(Channel::getWaveForm).map(WaveForm::getDurationMillis).reduce(Math::max)
                    .orElseGet(() -> Long.MIN_VALUE) > samples.timeStampMillis;
        }

        @Override
        public Samples next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                samples.timeStampMillis = nextTimeStamp(samples.timeStampMillis);
                getSamples(samples.timeStampMillis, samples.values);
                return samples;
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
            return samples.toString();
        }
    }

    public StimulationChannels() {
        this.channels = new ArrayList<>();
    }

    public StimulationChannels(List<Channel> channels) {
        this.channels = channels;
    }

    @Override
    public Iterator<Samples> iterator() {
        return new IteratorImpl();
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
