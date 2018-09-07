package teaselib.stimulation.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.Device;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.stimulation.pattern.SoundStimulation;
import teaselib.util.math.Partition;

public class IntentionBasedController<T extends Enum<?>, B extends Enum<?>> {
    private static final Logger logger = LoggerFactory.getLogger(IntentionBasedController.class);

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

    // TODO Add support for playAll(), append()
    
    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, double durationSeconds) {
        long startMillis = 0;
        long durationMillis = TimeUnit.SECONDS.toMillis((long) durationSeconds);
        play(allTargets(intention, stimulation, startMillis, durationMillis));
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2) {
        return play(intention, stimulation, intention2, stimulation2, 0);
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, double durationSeconds) {
        List<StimulationTarget> targets = new ArrayList<>();
        long startMillis = 0;
        long durationMillis = TimeUnit.SECONDS.toMillis((long) durationSeconds);
        targets.addAll(allTargets(intention, stimulation, startMillis, durationMillis));
        targets.addAll(allTargets(intention2, stimulation2, startMillis, durationMillis));
        play(targets);
        return this;
    }

    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3) {
        return play(intention, stimulation, intention2, stimulation2, intention3, stimulation3, 0);
    }

    // TODO Play all parallel, because playing one after another can be accomplished via sequential play/complete pairs
    // TODO Add overload with duration seconds parameter for each stimulation
    // -> allows to extend the target duration when playing
    // TODO Add overload with start offset parameter for each stimulation
    // TODO split into start and play, where play completes the stimulation
    public IntentionBasedController<T, B> play(T intention, Stimulation stimulation, T intention2,
            Stimulation stimulation2, T intention3, Stimulation stimulation3, double durationSeconds) {
        List<StimulationTarget> targets = new ArrayList<>();
        long startMillis = 0;
        long durationMillis = TimeUnit.SECONDS.toMillis((long) durationSeconds);
        targets.addAll(allTargets(intention, stimulation, startMillis, durationMillis));
        targets.addAll(allTargets(intention2, stimulation2, startMillis, durationMillis));
        targets.addAll(allTargets(intention3, stimulation3, startMillis, durationMillis));
        play(targets);
        return this;
    }

    private List<StimulationTarget> allTargets(T intention, Stimulation stimulation, long startMillis,
            long durationMillis) {
        if (stimulation instanceof SoundStimulation) {
            ((SoundStimulation) stimulation).play();
        }

        List<StimulationTarget> targets = new ArrayList<>();
        for (Stimulator stimulator : stimulators(intention)) {
            WaveForm waveform = stimulation.waveform(stimulator, intensity);
            targets.add(new StimulationTarget(stimulator, waveform, startMillis, durationMillis));
        }
        return targets;
    }

    void play(List<StimulationTarget> targets) {
        Partition<StimulationTarget> devices = new Partition<>(targets,
                (a, b) -> a.stimulator.getDevice() == b.stimulator.getDevice());
        for (Partition<StimulationTarget>.Group group : devices.groups) {
            play(group.get(0).stimulator.getDevice(), group);
        }
    }

    private void play(StimulationDevice device, Partition<StimulationTarget>.Group targetGroup) {
        StimulationTargets stimulationTargets = new StimulationTargets(device, asList(targetGroup));
        play(device, stimulationTargets);
    }

    private static List<StimulationTarget> asList(Iterable<StimulationTarget> lines) {
        List<StimulationTarget> list = new ArrayList<>();
        for (StimulationTarget line : lines) {
            list.add(line);
        }
        return list;
    }

    void play(StimulationDevice device, StimulationTargets targets) {
        logger.info("{} {}", device, targets);
        device.play(targets);
    }

    public void complete() {
        for (Entry<T, List<Stimulator>> entry : stims.entrySet()) {
            complete(entry.getKey());
        }
    }

    public void complete(T intention) {
        for (Stimulator stimulator : stimulators(intention)) {
            stimulator.getDevice().complete();
        }
    }

    public void stop() {
        for (Entry<T, List<Stimulator>> entry : stims.entrySet()) {
            stop(entry.getKey());
        }
    }

    public void stop(T intention) {
        play(intention, Stimulation.NONE);
    }

    private List<Stimulator> stimulators(T intention) {
        return stims.getOrDefault(intention, Collections.emptyList());
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
