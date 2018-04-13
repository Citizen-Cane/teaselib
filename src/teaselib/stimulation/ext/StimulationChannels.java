package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import teaselib.stimulation.WaveForm;

public class StimulationChannels implements Iterable<Channel> {
    final List<Channel> channels;

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

    public long maxDurationMillis() {
        Optional<Channel> max = channels.stream().reduce(Channel::maxDuration);
        if (max.isPresent()) {
            return max.get().waveForm.getDurationMillis();
        } else {
            throw new IllegalStateException();
        }
    }

    public long nextTimeStamp(long currentTimeMillis) {
        Optional<Long> min = channels.stream().map(Channel::getWaveForm)
                .map((WaveForm waveform) -> waveform.nextTime(currentTimeMillis)).reduce(Math::min);
        if (min.isPresent()) {
            return min.get();
        } else {
            throw new IllegalArgumentException(channels.toString());
        }
    }

    public void getSamples(long timeStampMillis, double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = channels.get(i).getValue(timeStampMillis);
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
}
