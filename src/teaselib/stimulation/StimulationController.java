package teaselib.stimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.Device;
import teaselib.stimulation.Stimulator.ChannelDependency;

/**
 * @author Citizen-Cane
 *
 *         Bookkeeping for available stimulations on various body parts.
 */
public class StimulationController<T> {
    private final static Logger logger = LoggerFactory
            .getLogger(StimulationController.class);

    private static final int SleepTimeForPartiallyDependentStimChannels = 100;
    private final Map<StimulationRegion, Stimulator> stimulators = new HashMap<StimulationRegion, Stimulator>();
    private final Map<T, Stimulation> stimulations = new HashMap<T, Stimulation>();

    private int intensity;

    private final Map<T, Stimulation> playing = new HashMap<T, Stimulation>();

    public StimulationController() {
        clear();
    }

    public void clear() {
        resetIntensity();
    }

    public void resetIntensity() {
        intensity = 1;
    }

    public void increaseIntensity() {
        intensity++;
    }

    public int intensity() {
        return intensity;
    }

    public void addRegion(StimulationRegion region, Stimulator stimulator) {
        stimulators.put(region, stimulator);
    }

    public Set<StimulationRegion> regions() {
        return stimulators.keySet();
    }

    public Stimulator stimulator(StimulationRegion region) {
        if (stimulators.containsKey(region)) {
            return stimulators.get(region);
        } else {
            throw new IllegalArgumentException(
                    "No stimulator assigned to body region "
                            + region.toString());
        }
    }

    public void add(T type, Stimulation stimulation) {
        stimulations.put(type, stimulation);
    }

    public boolean canStimulate(T type) {
        return stimulations.containsKey(type);
    }

    public boolean canStimulate(StimulationRegion region) {
        return stimulators.containsKey(region);
    }

    public void stimulate(T type) {
        stimulate(type, 0.0);
    }

    public void stimulate(T type, double durationSeconds) {
        synchronized (playing) {
            if (canStimulate(type)) {
                stopStimulation(type);
                Stimulation stimulation = stimulations.get(type);
                durationSeconds = ensureStimulationIsPlayedAtLeastOnce(
                        stimulation, durationSeconds);
                durationSeconds = handleChannelDependencies(stimulation,
                        durationSeconds);
                stimulation.play(intensity, durationSeconds);
                playing.put(type, stimulation);
            } else {
                logger.warn("Stimulation type " + type.toString()
                        + " hasn't been assigned to a body region");
            }
        }
    }

    private static double ensureStimulationIsPlayedAtLeastOnce(
            Stimulation stimulation, double durationSeconds) {
        if (durationSeconds == 0) {
            durationSeconds = stimulation.periodDurationSeconds;
        }
        return durationSeconds;
    }

    private void stopStimulation(T type) {
        if (playing.containsKey(type)) {
            Stimulation stimulation = playing.get(type);
            stimulation.stop();
            playing.remove(type);
        }
    }

    private double handleChannelDependencies(Stimulation stimulation,
            double durationSeconds) {
        ChannelDependency channelDependency = stimulation.stimulator
                .channelDependency();
        if (channelDependency != Stimulator.ChannelDependency.Independent) {
            for (Stimulation s : playing.values()) {
                if (s.stimulator.getDevice() == stimulation.stimulator
                        .getDevice()) {
                    if (channelDependency == Stimulator.ChannelDependency.PartiallyDependent) {
                        try {
                            Thread.sleep(
                                    SleepTimeForPartiallyDependentStimChannels);
                        } catch (InterruptedException ignore) {
                        }
                        if (durationSeconds > SleepTimeForPartiallyDependentStimChannels) {
                            durationSeconds -= SleepTimeForPartiallyDependentStimChannels;
                        }
                        break;
                    } else {
                        durationSeconds = completePreviousStimulation(s,
                                durationSeconds);
                        break;
                    }
                }
            }
        }
        return durationSeconds;
    }

    private static double completePreviousStimulation(Stimulation stimulation,
            double durationSeconds) {
        long start = System.currentTimeMillis();
        stimulation.complete();
        long now = System.currentTimeMillis();
        double sleepTimeSeconds = 1.0 / 1000.0 * (now - start);
        if (durationSeconds > sleepTimeSeconds) {
            durationSeconds -= sleepTimeSeconds;
        }
        return durationSeconds;
    }

    public void complete(T type) {
        synchronized (playing) {
            if (stimulations.containsKey(type)) {
                if (playing.containsKey(type)) {
                    playing.get(type).complete();
                }
            }
        }
    }

    public void complete() {
        synchronized (playing) {
            for (T type : playing.keySet()) {
                complete(type);
            }
        }
    }

    public void stop() {
        synchronized (playing) {
            for (Stimulation stimulation : playing.values()) {
                stimulation.stop();
            }
            playing.clear();
        }
    }

    public void close() {
        Set<Device> closed = new HashSet<Device>();
        for (Stimulator stimulator : stimulators.values()) {
            StimulationDevice device = stimulator.getDevice();
            if (!closed.contains(device)) {
                device.close();
                closed.add(device);
            }
        }
    }
}
