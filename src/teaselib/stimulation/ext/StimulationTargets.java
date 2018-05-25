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
        final int[] repeatCounts;

        private SampleIterator() {
            this.samples = new Samples(size());

            this.iterators = new ArrayList<>(size());
            this.waveformSamples = new ArrayList<>(size());
            this.repeatCounts = new int[size()];

            for (StimulationTarget target : targets) {
                waveformSamples.add(null);
                Iterator<WaveForm.Sample> iterator = target.getWaveForm().iterator();
                iterators.add(iterator);

                Sample sample = iterator.next();
                int targetIndex = targets.indexOf(target);
                samples.getValues()[targetIndex] = sample.getValue();
                waveformSamples.set(targetIndex, sample);

                repeatCounts[iterators.size() - 1] = 0;
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
                if (sample.isPresent()) {
                    int targetIndex = waveformSamples.indexOf(sample.get());
                    long duration = targets.get(targetIndex).waveForm.getDurationMillis() * repeatCounts[targetIndex];
                    if (duration + sample.get().getTimeStampMillis() >= samples.timeStampMillis) {
                        Sample next = sample.get();
                        long nextTimeStampMillis = next.getTimeStampMillis();
                        samples.timeStampMillis = duration + nextTimeStampMillis;
                        advance(nextTimeStampMillis);
                        return samples;
                    } else {
                        throw new IllegalStateException();
                    }
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

        private void fetchNextValue(int targetIndex) {
            Iterator<Sample> iterator = iterators.get(targetIndex);
            if (iterator != null) {
                boolean hasNext = iterator.hasNext();
                if (hasNext) {
                    waveformSamples.set(targetIndex, iterator.next());
                } else {
                    StimulationTarget target = targets.get(targetIndex);
                    if (repeatCounts[targetIndex] < target.repeatCount - 1) {
                        iterator = target.getWaveForm().iterator();
                        iterators.set(targetIndex, iterator);

                        Sample sample = iterator.next();
                        waveformSamples.set(targetIndex, sample);

                        repeatCounts[targetIndex]++;
                    } else {
                        long timeStamp = target.getWaveForm().getDurationMillis();
                        waveformSamples.set(targetIndex, new WaveForm.Sample(timeStamp, 0.0));
                        iterators.set(targetIndex, null);
                    }
                }
            } else {
                waveformSamples.set(targetIndex, WaveForm.Sample.End);
            }
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
