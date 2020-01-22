package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.stimulation.WaveForm.Sample;
import teaselib.stimulation.ext.StimulationTargets.Samples;

public class StimulationTargets implements Iterable<Samples> {
    public static final StimulationTargets None = new StimulationTargets();

    private final List<Stimulator> stimulators;
    final List<StimulationTarget> targets;

    public static class Samples {
        private long timeStampMillis;
        private long durationMillis;
        private final double[] values;

        public Samples(int channels) {
            timeStampMillis = Long.MIN_VALUE;
            values = new double[channels];
        }

        void setTimeStampMilis(long timeStampMillis, long durationMillis) {
            this.timeStampMillis = timeStampMillis;
            this.durationMillis = durationMillis;
        }

        public long getTimeStampMillis() {
            return timeStampMillis;
        }

        public long getDurationMillis() {
            return durationMillis;
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
        private final Samples samples;

        private final List<Iterator<WaveForm.Sample>> iterators;
        private final List<WaveForm.Sample> waveformSamples;
        private final int[] repeatCounts;

        private Optional<Sample> next;

        SampleIterator() {
            this.samples = new Samples(size());

            this.iterators = new ArrayList<>(size());
            this.waveformSamples = new ArrayList<>(size());
            this.repeatCounts = new int[size()];

            for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
                StimulationTarget target = targets.get(targetIndex);
                waveformSamples.add(null);
                Iterator<WaveForm.Sample> iterator = target.getWaveForm().iterator();
                if (iterator.hasNext()) {
                    iterators.add(iterator);
                    Sample sample = iterator.next();
                    samples.getValues()[targetIndex] = sample.getValue();
                    waveformSamples.set(targetIndex, sample);
                    repeatCounts[iterators.size() - 1] = 0;
                } else {
                    iterators.add(null);
                    Sample sample = new Sample(0, 0.0);
                    samples.getValues()[targetIndex] = sample.getValue();
                    waveformSamples.set(targetIndex, sample);
                    repeatCounts[iterators.size() - 1] = 0;
                }
            }

            next = getNextSample();
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
                if (next.isPresent()) {
                    Sample sample = next.get();
                    int targetIndex = waveformSamples.indexOf(sample);
                    long waveFormDuration = targets.get(targetIndex).waveForm.getDurationMillis()
                            * repeatCounts[targetIndex];
                    long timeStampMillis = sample.getTimeStampMillis();
                    if (waveFormDuration + timeStampMillis >= samples.getTimeStampMillis()) {
                        advance(timeStampMillis);
                        next = getNextSample();
                        if (next.isPresent()) {
                            long nextWaveFormDuration = targets.get(targetIndex).waveForm.getDurationMillis()
                                    * repeatCounts[targetIndex];
                            long nextTimeStampMillis = next.get().getTimeStampMillis();

                            // TODO waveform duration is finite, so the infinite delay is irregular -> remove
                            long durationMillis = nextTimeStampMillis == Long.MAX_VALUE ? Long.MAX_VALUE
                                    : (nextWaveFormDuration + nextTimeStampMillis)
                                            - (waveFormDuration + timeStampMillis);
                            samples.setTimeStampMilis(waveFormDuration + timeStampMillis, durationMillis);
                            return samples;
                        } else {
                            throw new IllegalStateException();
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        private Optional<Sample> getNextSample() {
            return waveformSamples.stream().reduce(Sample::earliest);
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

    private StimulationTargets() {
        this(new ArrayList<>());
    }

    public StimulationTargets(StimulationDevice device) {
        this(device.stimulators());
    }

    public StimulationTargets(StimulationDevice device, List<StimulationTarget> targets) {
        this(device.stimulators());
        for (int i = 0; i < targets.size(); i++) {
            StimulationTarget target = targets.get(i);
            if (target != StimulationTarget.EMPTY) {
                set(target);
            } else {
                clear(i);
            }
        }
    }

    private StimulationTargets(List<Stimulator> stimulators) {
        this.stimulators = stimulators;
        this.targets = new ArrayList<>(stimulators.size());
        int size = stimulators.size();
        for (int i = 0; i < size; i++) {
            targets.add(StimulationTarget.EMPTY);
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

    public void set(Stimulator stimulator, WaveForm waveForm, long duration, TimeUnit timeUnit) {
        set(new StimulationTarget(stimulator, waveForm, timeUnit.toMillis(duration)));
    }

    public void set(StimulationTarget target) {
        int index = stimulators.indexOf(target.stimulator);
        if (index >= 0) {
            targets.set(index, target);
        } else {
            throw new IllegalArgumentException("Target belongs to different device: " + target);
        }
    }

    public void clear(Stimulator stimulator) {
        if (stimulator == null)
            throw new IllegalArgumentException();

        int index = stimulators.indexOf(stimulator);
        if (index >= 0) {
            targets.set(index, StimulationTarget.EMPTY);
        } else {
            throw new IllegalArgumentException("Stimulator belongs to different device: " + stimulator);
        }
    }

    private void clear(int index) {
        targets.set(index, StimulationTarget.EMPTY);
    }

    @Override
    public String toString() {
        return targets.toString();
    }

    public StimulationTargets continuedStimulation(StimulationTargets replacement, long startMillis) {
        Objects.requireNonNull(replacement);
        if (size() != replacement.size()) {
            throw new IllegalArgumentException(replacement.toString());
        }

        StimulationTargets continuation = new StimulationTargets(stimulators);
        for (int i = 0; i < stimulators.size(); i++) {
            Stimulator stimulator = stimulators.get(i);
            if (stimulator != replacement.stimulators.get(i))
                throw new IllegalArgumentException(replacement.stimulators.toString());

            StimulationTarget newTarget = replacement.get(i);
            StimulationTarget oldTarget = targets.get(i);
            if (oldTarget == StimulationTarget.EMPTY || startMillis >= oldTarget.getWaveForm().getDurationMillis()) {
                if (newTarget == StimulationTarget.EMPTY) {
                    continuation.clear(i);
                } else {
                    continuation.set(newTarget);
                }
            } else if (newTarget == StimulationTarget.EMPTY) {
                continuation.set(oldTarget.slice(startMillis));
            } else {
                continuation.set(newTarget.slice(startMillis));
            }
        }

        return continuation;
    }
}
