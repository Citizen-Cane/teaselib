package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
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
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation)));
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2) {
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation),
                new StimulationCommand(stims.get(intention2), stimulation2)));
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3) {
        play(Arrays.asList(new StimulationCommand(stims.get(intention), stimulation),
                new StimulationCommand(stims.get(intention2), stimulation2),
                new StimulationCommand(stims.get(intention3), stimulation3)));
    }

    public void play(List<StimulationCommand> stimulationCommands) {
        Partition<StimulationCommand> devices = new Partition<>(stimulationCommands,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<StimulationCommand>.Group group : devices.groups) {
            play(group);
        }
    }

    private void play(Partition<StimulationCommand>.Group group) {
        List<Channel> channels = new ArrayList<>(group.size());
        for (StimulationCommand stimulationCommand : group) {
            Stimulator stimulator = stimulationCommand.stimulator;
            WaveForm waveForm = stimulationCommand.stimulation.getWaveform(stimulator, intensity);
            channels.add(new Channel(stimulator, waveForm));
        }
        StimulationDevice device = group.get(0).stimulator.getDevice();
        play(device, channels);
    }

    void play(StimulationDevice device, List<Channel> channels) {
        device.play(channels);
    }
}
