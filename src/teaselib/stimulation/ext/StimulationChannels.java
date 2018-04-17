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

        private IteratorImpl() {
            this.samples = new Samples(size());
            this.waveformSamples = new ArrayList<>(size());
            this.iterators = new ArrayList<>(size());

            for (Channel channel : channels) {
                Iterator<WaveForm.Sample> iterator = channel.getWaveForm().iterator();
                iterators.add(iterator);
                Sample sample = iterator.next();
                samples.getValues()[waveformSamples.size()] = sample.getValue();
                waveformSamples.add(sample);
            }
        }

        @Override
        public boolean hasNext() {
            return waveformSamples.stream().filter(sample -> sample == WaveForm.Sample.End).count() < channels.size();
        }

        @Override
        public Samples next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                Optional<Sample> sample = waveformSamples.stream().reduce(Sample::earliest);
                if (sample.isPresent() && sample.get().getTimeStampMillis() >= samples.timeStampMillis) {
                    Sample next = sample.get();
                    long nextTimeStampMillis = next.getTimeStampMillis();
                    samples.timeStampMillis = nextTimeStampMillis;
                    advance(nextTimeStampMillis);
                    return samples;
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        private void advance(long nextTimeStampMillis) {
            for (int channel = 0; channel < channels.size(); channel++) {
                Sample sample = waveformSamples.get(channel);
                if (sample.getTimeStampMillis() == nextTimeStampMillis) {
                    samples.getValues()[channel] = sample.getValue();
                    fetchNextValue(channel);
                }
            }
        }

        private void fetchNextValue(int channel) {
            Iterator<Sample> iterator = iterators.get(channel);
            if (iterator != null) {
                boolean hasNext = iterator.hasNext();
                if (hasNext) {
                    waveformSamples.set(channel, iterator.next());
                } else {
                    waveformSamples.set(channel, new WaveForm.Sample(channelDuration(channel), 0.0));
                    iterators.set(channel, null);
                }
            } else {
                waveformSamples.set(channel, WaveForm.Sample.End);
            }
        }

        private long channelDuration(int channel) {
            return get(channel).getWaveForm().getDurationMillis();
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
