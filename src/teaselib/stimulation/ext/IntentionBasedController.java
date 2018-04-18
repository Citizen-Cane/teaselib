package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.util.math.Partition;

class IntentionBasedController<T extends Enum<?>> {
    private final Map<T, List<Stimulator>> stims = new HashMap<>();

    private int intensity = Stimulation.MinIntensity;

    public void clear() {
        stims.clear();
    }

    public void add(T intention, Stimulator... stimulators) {
        List<Stimulator> list = stims.computeIfAbsent(intention, (key) -> new ArrayList<>());
        list.addAll(Arrays.asList(stimulators));
    }

    public void play(T intention, Stimulation stimulation) {
        play(intention, stimulation, 0);
    }

    public void play(T intention, Stimulation stimulation, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        channels.addAll(newChannels(intention, stimulation, 0));
        play(channels, durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2) {
        play(intention, stimulation, intention2, stimulation2, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2,
            double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        List<Channel> channels1 = newChannels(intention, stimulation, 0);
        channels.addAll(channels1);
        List<Channel> channels2 = newChannels(intention2, stimulation2, getMaxDuration(channels1));
        channels.addAll(channels2);
        play(channels, durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3) {
        play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        List<Channel> channels1 = newChannels(intention, stimulation, 0);
        channels.addAll(channels1);
        List<Channel> channels2 = newChannels(intention2, stimulation2, getMaxDuration(channels1));
        channels.addAll(channels2);
        channels.addAll(newChannels(intention3, stimulation3, getMaxDuration(channels2)));
        play(channels, durationSeconds);
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
            WaveForm waveform = stimulation.getWaveform(stimulator, intensity);
            newChannels.add(new Channel(stimulator, waveform, startMillis));
        }
        return newChannels;
    }

    void play(List<Channel> channels, double durationSeconds) {
        Partition<Channel> devices = new Partition<>(channels,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<Channel>.Group group : devices.groups) {
            play(group, durationSeconds);
        }
    }

    private void play(Partition<Channel>.Group group, double durationSeconds) {
        StimulationChannels channels = new StimulationChannels(asList(group));
        int repeatCount = repeatCount(channels, durationSeconds);
        play(channels.get(0).stimulator.getDevice(), channels, repeatCount);
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
}
