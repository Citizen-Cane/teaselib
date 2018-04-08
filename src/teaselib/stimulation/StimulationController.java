package teaselib.stimulation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.devices.Device;
import teaselib.stimulation.Stimulator.ChannelDependency;

/**
 * Bookkeeping for available stimulations on various body parts.
 *
 * @author Citizen-Cane
 */
public abstract class StimulationController<T> {
    private static final Logger logger = LoggerFactory.getLogger(StimulationController.class);

    private static final int SLEEP_TIME_FOR_PARTIALLY_DEPENDENT_CHANNELS = 100;

    private final Map<StimulationRegion, Stimulator> regions = new EnumMap<>(StimulationRegion.class);
    private final Map<T, Stimulation> stimulations = new HashMap<>();
    private final Map<T, Stimulator> stimulators = new HashMap<>();
    private final Map<T, Stimulation> playing = new HashMap<>();

    private int intensity;

    public StimulationController() {
        clear();
    }

    public void clear() {
        stop();
        resetIntensity();
        regions.clear();
        stimulators.clear();
        stimulations.clear();
    }

    public void resetIntensity() {
        intensity = Stimulation.MinIntensity;
    }

    public void increaseIntensity() {
        if (intensity < Stimulation.MaxIntensity) {
            intensity++;
        }
    }

    public int intensity() {
        return intensity;
    }

    public void addRegion(StimulationRegion region, Stimulator stimulator) {
        regions.put(region, stimulator);
    }

    public Set<StimulationRegion> regions() {
        return regions.keySet();
    }

    public Stimulator stimulator(StimulationRegion region) {
        if (regions.containsKey(region)) {
            return regions.get(region);
        } else {
            throw new IllegalArgumentException("No stimulator assigned to body region " + region.toString());
        }
    }

    public Stimulator stimulator(T type) {
        if (regions.containsKey(type)) {
            return regions.get(type);
        } else {
            throw new IllegalArgumentException("No stimulator assigned to stimulation type " + type.toString());
        }
    }

    public void add(T type, Stimulation stimulation, Stimulator stimulator) {
        stimulations.put(type, stimulation);
        stimulators.put(type, stimulator);
    }

    public boolean canStimulate(T type) {
        return stimulations.containsKey(type);
    }

    public boolean canStimulate(StimulationRegion region) {
        return regions.containsKey(region);
    }

    public Set<T> getStimulations(StimulationRegion region) {
        Set<T> stimulationsInRegion = new LinkedHashSet<>();
        for (Entry<T, Stimulator> entry : stimulators.entrySet()) {
            if (entry.getValue() == stimulator(region)) {
                stimulationsInRegion.add(entry.getKey());
            }
        }
        return stimulationsInRegion;
    }

    public void stimulate(T type) {
        stimulate(type, 0.0);
    }

    public void stimulate(T type, double durationSeconds) {
        synchronized (playing) {
            if (canStimulate(type)) {
                Stimulator stimulator = stimulator(type);
                Stimulation stimulation = stimulations.get(type);

                double actualDurationSeconds = durationSeconds;
                actualDurationSeconds -= completeHigherPriorityStimulation(type, stimulation);
                actualDurationSeconds -= handleChannelDependencies(stimulator, stimulation);
                actualDurationSeconds = Math.max(0, actualDurationSeconds);

                if (logger.isInfoEnabled()) {
                    logger.info(getClass().getSimpleName() + ": intensity=" + intensity + " duration=" + durationSeconds
                            + " on " + stimulator.getDeviceName() + ", " + stimulator.getLocation());
                }

                WaveForm waveform = stimulation.getWaveform(stimulator(type), intensity);
                stimulator.play(waveform, actualDurationSeconds, intensity);
                playing.put(type, stimulation);
            } else {
                logger.warn("Stimulation type {} hasn't been assigned to a body region", type);
            }
        }
    }

    private double completeHigherPriorityStimulation(T type, Stimulation stimulation) {
        if (playing.containsKey(type)) {
            double startMillis = System.currentTimeMillis();
            Stimulation current = playing.get(type);
            if (current.priority > stimulation.priority) {
                stimulator(type).complete();
            } else {
                stimulator(type).stop();
            }
            playing.remove(type);
            return System.currentTimeMillis() - startMillis;
        } else {
            return 0.0;
        }
    }

    public void stopStimulation(T type) {
        if (playing.containsKey(type)) {
            stimulator(type).stop();
            playing.remove(type);
        }
    }

    private double handleChannelDependencies(Stimulator stimulator, Stimulation stimulation) {
        double delay = 0;
        ChannelDependency channelDependency = stimulator.channelDependency();
        if (channelDependency != Stimulator.ChannelDependency.Independent) {
            for (Entry<T, Stimulation> entry : playing.entrySet()) {
                if (stimulator(entry.getKey()).getDevice() == stimulator.getDevice()) {
                    if (channelDependency == Stimulator.ChannelDependency.PartiallyDependent) {
                        delay = sleep(SLEEP_TIME_FOR_PARTIALLY_DEPENDENT_CHANNELS) * 1000.0;
                    } else {
                        delay = completePreviousStimulation(entry.getKey());
                    }
                    break;
                }
            }
        }
        return delay;
    }

    private long sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
        return delayMillis;

    }

    private double completePreviousStimulation(T type) {
        long start = System.currentTimeMillis();
        stimulator(type).complete();
        long now = System.currentTimeMillis();
        return (now - start) / 1000.0;
    }

    public void complete(T type) {
        synchronized (playing) {
            if (stimulations.containsKey(type) && playing.containsKey(type)) {
                stimulator(type).complete();
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
            for (T type : playing.keySet()) {
                stimulator(type).stop();
            }
            playing.clear();
        }
    }

    public void close() {
        synchronized (playing) {
            stop();
            Set<Device> closed = new HashSet<>();
            for (Stimulator stimulator : regions.values()) {
                StimulationDevice device = stimulator.getDevice();
                if (!closed.contains(device)) {
                    device.close();
                    closed.add(device);
                }
            }
        }
    }
}
