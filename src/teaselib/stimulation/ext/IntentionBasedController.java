package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import teaselib.core.devices.Device;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.util.math.Partition;

public class IntentionBasedController<T extends Enum<?>, B extends Enum<?>> {
    private final Map<T, List<Stimulator>> stims = new HashMap<>();

    private final Map<T, B> regions = new HashMap<>();

    private int intensity = Stimulation.MinIntensity;

    public void clear() {
        stims.clear();
    }

    public void add(T intention, Stimulator... stimulators) {
        List<Stimulator> list = stims.computeIfAbsent(intention, (key) -> new ArrayList<>());
        list.addAll(Arrays.asList(stimulators));
    }

    public void assign(T intention, B region) {
        regions.put(intention, region);
    }

    // TODO Implies there's a single stimulator for each intention -> allow for multiple (spank butt, smack balls, etc.)
    public B is(T intention) {
        return regions.get(intention);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation) {
        return play(intention, stimulation, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        channels.addAll(newChannels(intention, stimulation, 0));
        play(channels, durationSeconds);
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2) {
        return play(intention, stimulation, intention2, stimulation2, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        List<Channel> channels1 = newChannels(intention, stimulation, 0);
        channels.addAll(channels1);
        List<Channel> channels2 = newChannels(intention2, stimulation2, getMaxDuration(channels1));
        channels.addAll(channels2);
        play(channels, durationSeconds);
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3) {
        return play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        List<Channel> channels1 = newChannels(intention, stimulation, 0);
        channels.addAll(channels1);
        List<Channel> channels2 = newChannels(intention2, stimulation2, getMaxDuration(channels1));
        channels.addAll(channels2);
        channels.addAll(newChannels(intention3, stimulation3, getMaxDuration(channels2)));
        play(channels, durationSeconds);
        return this;
    }

    private long getMaxDuration(List<Channel> channels) {
        Optional<Channel> duration = channels.stream().reduce(Channel::maxDuration);
        if (duration.isPresent()) {
            return duration.get().getWaveForm().getDurationMillis();
        } else {
            throw new IllegalArgumentException(channels.toString());
        }
    }

    private List<Channel> newChannels(T intention, Stimulation stimulation, long startMillis) {
        List<Stimulator> stimulators = stims.get(intention);
        List<Channel> newChannels = new ArrayList<>();
        for (Stimulator stimulator : stimulators) {
            WaveForm waveform = stimulation.waveform(stimulator, intensity);
            newChannels.add(new Channel(stimulator, waveform, startMillis));
        }
        return newChannels;
    }

    void play(List<Channel> channels, double durationSeconds) {
        Partition<Channel> devices = new Partition<>(channels,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<Channel>.Group group : devices.groups) {
            play(group.get(0).stimulator.getDevice(), group, durationSeconds);
        }
    }

    private void play(StimulationDevice device, Partition<Channel>.Group group, double durationSeconds) {
        StimulationChannels channels = new StimulationChannels(asList(group));
        int repeatCount = repeatCount(channels, durationSeconds);
        play(device, channels, repeatCount);
    }

    private List<Channel> asList(Iterable<Channel> group) {
        List<Channel> channels = new ArrayList<>();
        for (Channel channel : group) {
            channels.add(channel);
        }
        return channels;
    }

    private int repeatCount(StimulationChannels channels, double durationSeconds) {
        long maxDurationMillis = channels.maxDurationMillis();
        return maxDurationMillis > 0 ? Math.max(1, (int) (WaveForm.toMillis(durationSeconds) / maxDurationMillis)) : 1;
    }

    void play(StimulationDevice device, StimulationChannels channels, int repeatCount) {
        device.play(channels, repeatCount);
    }

    public void complete() {
        for (Entry<T, List<Stimulator>> entry : stims.entrySet()) {
            complete(entry.getKey());
        }
    }

    public void complete(T intention) {
        for (Stimulator stimulator : stims.get(intention)) {
            stimulator.getDevice().complete();
        }
    }

    public void stop() {
        for (Entry<T, List<Stimulator>> entry : stims.entrySet()) {
            stop(entry.getKey());
        }
    }

    public void stop(T intention) {
        for (Stimulator stimulator : stims.get(intention)) {
            stimulator.getDevice().stop();
        }
    }

    public void increaseIntensity() {
        if (intensity < Stimulation.MaxIntensity) {
            intensity++;
        }
    }

    public void close() {
        stop();
        Set<Device> closed = new HashSet<>();
        for (Entry<T, List<Stimulator>> entry : stims.entrySet()) {
            for (Stimulator stimulator : entry.getValue()) {
                StimulationDevice device = stimulator.getDevice();
                if (!closed.contains(device)) {
                    device.close();
                    closed.add(device);
                }
            }
        }
    }
}
