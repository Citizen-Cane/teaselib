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

    public class SampleIterator implements Iterator<double[]> {
        final double[] values;
        long timeStampMillis;

        private SampleIterator() {
            values = new double[channels.size()];
            timeStampMillis = 0;
        }

        public long getTimeStampMillis() {
            return timeStampMillis;
        }

        @Override
        public boolean hasNext() {
            // TODO Fails to detect end because the waveform only returns Long.MAX_VALUE when called once more
            // return nextTimeStamp(timeStampMillis) < Long.MAX_VALUE;

            return channels.stream().map(Channel::getWaveForm).map(WaveForm::getDurationMillis).reduce(Math::max)
                    .get() > timeStampMillis;
        }

        @Override
        public double[] next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            } else {
                getSamples(timeStampMillis, values);
                timeStampMillis = nextTimeStamp(timeStampMillis);
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
