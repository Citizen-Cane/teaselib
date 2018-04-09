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
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation)), durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2) {
        play(intention, stimulation, intention2, stimulation2, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2,
            double durationSeconds) {
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation),
                new StimulationCommand(stims.get(intention2), stimulation2)), durationSeconds);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3) {
        play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3, double durationSeconds) {
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation),
                new StimulationCommand(stims.get(intention2), stimulation2),
                new StimulationCommand(stims.get(intention3), stimulation3)), durationSeconds);
    }

    public void play(List<StimulationCommand> stimulationCommands, double durationSeconds) {
        Partition<StimulationCommand> devices = new Partition<>(stimulationCommands,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<StimulationCommand>.Group group : devices.groups) {
            play(group, durationSeconds);
        }
    }

    private void play(Partition<StimulationCommand>.Group group, double durationSeconds) {
        List<Channel> channels = new ArrayList<>(group.size());
        for (StimulationCommand stimulationCommand : group) {
            Stimulator stimulator = stimulationCommand.stimulator;
            WaveForm waveForm = stimulationCommand.stimulation.getWaveform(stimulator, intensity);
            channels.add(new Channel(stimulator, waveForm));
        }
        StimulationDevice device = group.get(0).stimulator.getDevice();
        long maxDurationMillis = maxDurationMillis(channels);
        int repeatCount = maxDurationMillis > 0
                ? Math.max(1, (int) (WaveForm.toMillis(durationSeconds) / maxDurationMillis))
                : 1;
        play(device, channels, repeatCount);
    }

    private long maxDurationMillis(List<Channel> channels) {
        Optional<Channel> max = channels.stream().reduce(Channel::maxDuration);
        if (max.isPresent()) {
            return max.get().waveForm.getDuration();
        } else {
            throw new IllegalStateException();
        }
    }

    void play(StimulationDevice device, List<Channel> channels, int repeatCount) {
        device.play(channels, repeatCount);
    }
}
