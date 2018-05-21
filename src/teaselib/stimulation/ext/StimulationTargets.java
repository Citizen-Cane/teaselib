package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.stimulation.WaveForm.Sample;
import teaselib.stimulation.ext.StimulationTargets.Samples;

public class StimulationTargets implements Iterable<Samples> {
    private final List<Stimulator> stimulators;
    private final List<StimulationTarget> targets;

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

    private class SampleIterator implements Iterator<Samples> {
        final Samples samples;

        final List<Iterator<WaveForm.Sample>> iterators;
        final List<WaveForm.Sample> waveformSamples;

        private SampleIterator() {
            this.samples = new Samples(size());
            this.waveformSamples = new ArrayList<>(size());
            this.iterators = new ArrayList<>(size());

            for (StimulationTarget channel : targets) {
                Iterator<WaveForm.Sample> iterator = channel.getWaveForm().iterator();
                iterators.add(iterator);
                Sample sample = iterator.next();
                samples.getValues()[waveformSamples.size()] = sample.getValue();
                waveformSamples.add(sample);
            }
        }

        @Override
        public boolean hasNext() {
            return waveformSamples.stream().filter(sample -> sample == WaveForm.Sample.End).count() < targets.size();
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
            for (int channel = 0; channel < targets.size(); channel++) {
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

    public StimulationTargets(StimulationDevice device) {
        this.stimulators = device.stimulators();
        this.targets = new ArrayList<>(stimulators.size());
        int size = stimulators.size();
        for (int i = 0; i < size; i++) {
            targets.add(StimulationTarget.EMPTY);
        }
    }

    public StimulationTargets(StimulationDevice device, List<StimulationTarget> channels) {
        this(device);
        for (StimulationTarget channel : channels) {
            add(channel);
        }
    }

    @Override
    public Iterator<Samples> iterator() {
        return new SampleIterator();
    }

    public long maxDurationMillis() {
        Optional<StimulationTarget> max = targets.stream().reduce(StimulationTarget::maxDuration);
        if (max.isPresent()) {
            return max.get().waveForm.getDurationMillis();
        } else {
            throw new IllegalStateException();
        }
    }

    public int size() {
        return targets.size();
    }

    public Stream<StimulationTarget> stream() {
        return targets.stream();
    }

    public StimulationTarget get(int index) {
        return targets.get(index);
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public void add(StimulationTarget channel) {
        int index = stimulators.indexOf(channel.stimulator);
        if (index >= 0) {
            targets.set(index, channel);
        } else {
            throw new IllegalArgumentException("Channel belongs to differnet device: " + channel);
        }
    }

    @Override
    public String toString() {
        return targets.toString();
    }
}
