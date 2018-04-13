package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.util.math.Partition;

public class IntentionBasedController<T extends Enum<?>> {
    Map<T, Stimulator> stims = new HashMap<>();

    private int intensity = Stimulation.MinIntensity;

    public void clear() {
        stims.clear();
    }

    public void add(T intention, Stimulator stimulator) {
        stims.put(intention, stimulator);
    }

    public void play(T intention, Stimulation stimulation) {
        play(intention, stimulation, 0);
    }

    public void play(T intention, Stimulation stimulation, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        channels.add(newChannel(intention, stimulation, 0));
        play(channels, durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2) {
        play(intention, stimulation, intention2, stimulation2, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2,
            double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        Channel channel1 = newChannel(intention, stimulation, 0);
        channels.add(channel1);
        channels.add(newChannel(intention2, stimulation2, channel1.waveForm.getDurationMillis()));
        play(channels, durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3) {
        play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3, double durationSeconds) {
        List<Channel> channels = new ArrayList<>();
        Channel channel1 = newChannel(intention, stimulation, 0);
        channels.add(channel1);
        Channel channel2 = newChannel(intention2, stimulation2, channel1.waveForm.getDurationMillis());
        channels.add(channel2);
        channels.add(newChannel(intention3, stimulation3, channel2.waveForm.getDurationMillis()));
        play(channels, durationSeconds);
    }

    private Channel newChannel(T intention, Stimulation stimulation, long startMillis) {
        Stimulator stimulator = stims.get(intention);
        WaveForm waveform = stimulation.getWaveform(stimulator, intensity);
        return new Channel(stimulator, waveform, startMillis);
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
        return maxDurationMillis > 0
                ? Math.max(1, (int) (WaveForm.toMillis(durationSeconds) / maxDurationMillis))
                : 1;
    }

    void play(StimulationDevice device, StimulationChannels channels, int repeatCount) {
        device.play(channels, repeatCount);
    }
}
