package teaselib.stimulation.ext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.util.math.Partition;

public class IntentionBasedController<T extends Enum<?>> {
    Map<T, Stimulator> stims = new HashMap<>();

    public class StimulationAction {
        final Stimulator stimulator;
        final Stimulation stimulation;

        public StimulationAction(Stimulator stimulator, Stimulation stimulation) {
            this.stimulator = stimulator;
            this.stimulation = stimulation;
        }

        @Override
        public String toString() {
            return stimulator + "=" + stimulation;
        }
    }

    public void clear() {
        stims.clear();
    }

    public void add(T intention, Stimulator stimulator) {
        stims.put(intention, stimulator);
    }

    public void play(T intention, Stimulation stimulation) {
        play(Arrays.asList(new StimulationAction(stims.get(intention), stimulation)));
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2) {
        play(Arrays.asList(new StimulationAction(stims.get(intention), stimulation),
                new StimulationAction(stims.get(intention2), stimulation2)));
    }

    public void play(T intention, Stimulation stimulation, T intention2, Stimulation stimulation2, T intention3,
            Stimulation stimulation3) {
        play(Arrays.asList(new StimulationAction(stims.get(intention), stimulation),
                new StimulationAction(stims.get(intention2), stimulation2),
                new StimulationAction(stims.get(intention3), stimulation3)));
    }

    public void play(List<StimulationAction> stimulationActions) {
        Partition<StimulationAction> devices = new Partition<>(stimulationActions,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<StimulationAction>.Group group : devices.groups) {
            play(group);
        }
    }

    private void play(Partition<IntentionBasedController<T>.StimulationAction>.Group group) {
        StimulationDevice device = group.items.get(0).stimulator.getDevice();
        play(device, group.items);
    }

    void play(StimulationDevice device, List<IntentionBasedController<T>.StimulationAction> items) {
        // TODO render waveforms and upload to device
    }
}
