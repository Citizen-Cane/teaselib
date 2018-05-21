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
        List<Stimulator> list = stims.computeIfAbsent(intention, key -> new ArrayList<>());
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
        List<StimulationTarget> targets = new ArrayList<>();
        targets.addAll(allTargets(intention, stimulation, 0));
        play(targets, durationSeconds);
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2) {
        return play(intention, stimulation, intention2, stimulation2, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, double durationSeconds) {
        List<StimulationTarget> targets = new ArrayList<>();
        List<StimulationTarget> targets1 = allTargets(intention, stimulation, 0);
        targets.addAll(targets1);
        List<StimulationTarget> targets2 = allTargets(intention2, stimulation2, getMaxDuration(targets1));
        targets.addAll(targets2);
        play(targets, durationSeconds);
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3) {
        return play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3, double durationSeconds) {
        List<StimulationTarget> targets = new ArrayList<>();
        List<StimulationTarget> targets1 = allTargets(intention, stimulation, 0);
        targets.addAll(targets1);
        List<StimulationTarget> targets2 = allTargets(intention2, stimulation2, getMaxDuration(targets1));
        targets.addAll(targets2);
        targets.addAll(allTargets(intention3, stimulation3, getMaxDuration(targets2)));
        play(targets, durationSeconds);
        return this;
    }

    private static long getMaxDuration(List<StimulationTarget> targets) {
        Optional<StimulationTarget> duration = targets.stream().reduce(StimulationTarget::maxDuration);
        if (duration.isPresent()) {
            return duration.get().getWaveForm().getDurationMillis();
        } else {
            throw new IllegalArgumentException(targets.toString());
        }
    }

    private List<StimulationTarget> allTargets(T intention, Stimulation stimulation, long startMillis) {
        List<Stimulator> stimulators = stims.get(intention);
        List<StimulationTarget> targets = new ArrayList<>();
        for (Stimulator stimulator : stimulators) {
            WaveForm waveform = stimulation.waveform(stimulator, intensity);
            targets.add(new StimulationTarget(stimulator, waveform, startMillis));
        }
        return targets;
    }

    void play(List<StimulationTarget> targets, double durationSeconds) {
        Partition<StimulationTarget> devices = new Partition<>(targets,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<StimulationTarget>.Group group : devices.groups) {
            play(group.get(0).stimulator.getDevice(), group, durationSeconds);
        }
    }

    private void play(StimulationDevice device, Partition<StimulationTarget>.Group targetGroup, double durationSeconds) {
        StimulationTargets stimulationTargets = new StimulationTargets(device, asList(targetGroup));
        int repeatCount = repeatCount(stimulationTargets, durationSeconds);
        play(device, stimulationTargets, repeatCount);
    }

    private static List<StimulationTarget> asList(Iterable<StimulationTarget> lines) {
        List<StimulationTarget> list = new ArrayList<>();
        for (StimulationTarget line : lines) {
            list.add(line);
        }
        return list;
    }

    private static int repeatCount(StimulationTargets lines, double durationSeconds) {
        long maxDurationMillis = lines.maxDurationMillis();
        return maxDurationMillis > 0 ? Math.max(1, (int) (WaveForm.toMillis(durationSeconds) / maxDurationMillis)) : 1;
    }

    void play(StimulationDevice device, StimulationTargets lines, int repeatCount) {
        device.play(lines, repeatCount);
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
