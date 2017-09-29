package teaselib.stimulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
public class StimulationController<T> {
    private final static Logger logger = LoggerFactory.getLogger(StimulationController.class);

    private static final int SleepTimeForPartiallyDependentStimChannels = 100;

    private final Map<StimulationRegion, Stimulator> stimulators = new HashMap<>();
    private final Map<T, Stimulation> stimulations = new HashMap<>();
    private final Map<T, Stimulation> playing = new HashMap<>();

    private int intensity;

    public StimulationController() {
        clear();
    }

    public void clear() {
        resetIntensity();
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
        stimulators.put(region, stimulator);
    }

    public Set<StimulationRegion> regions() {
        return stimulators.keySet();
    }

    public Stimulator stimulator(StimulationRegion region) {
        if (stimulators.containsKey(region)) {
            return stimulators.get(region);
        } else {
            throw new IllegalArgumentException("No stimulator assigned to body region " + region.toString());
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
                Stimulation newStimulation = stimulations.get(type);

                double actualDurationSeconds = durationSeconds;
                actualDurationSeconds -= completeHigherPriorityStimulation(type, newStimulation);
                actualDurationSeconds -= handleChannelDependencies(newStimulation);
                actualDurationSeconds = Math.max(0, actualDurationSeconds);

                newStimulation.play(actualDurationSeconds, intensity);
                playing.put(type, newStimulation);
            } else {
                logger.warn("Stimulation type " + type.toString() + " hasn't been assigned to a body region");
            }
        }
    }

    private double completeHigherPriorityStimulation(T type, Stimulation newStimulation) {
        if (playing.containsKey(type)) {
            double startMillis = System.currentTimeMillis();
            Stimulation current = playing.get(type);
            if (current.priority > newStimulation.priority) {
                current.complete();
            } else {
                current.stop();
            }
            playing.remove(type);
            return System.currentTimeMillis() - startMillis;
        } else {
            return 0.0;
        }
    }

    public void stopStimulation(T type) {
        if (playing.containsKey(type)) {
            Stimulation stimulation = playing.get(type);
            stimulation.stop();
            playing.remove(type);
        }
    }

    private double handleChannelDependencies(Stimulation stimulation) {
        double delay = 0;
        ChannelDependency channelDependency = stimulation.stimulator.channelDependency();
        if (channelDependency != Stimulator.ChannelDependency.Independent) {
            for (Stimulation s : playing.values()) {
                if (s.stimulator.getDevice() == stimulation.stimulator.getDevice()) {
                    if (channelDependency == Stimulator.ChannelDependency.PartiallyDependent) {
                        try {
                            Thread.sleep(SleepTimeForPartiallyDependentStimChannels);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new ScriptInterruptedException(e);
                        }
                        delay = SleepTimeForPartiallyDependentStimChannels * 1000;
                        break;
                    } else {
                        delay = completePreviousStimulation(s);
                        break;
                    }
                }
            }
        }
        return delay;
    }

    private static double completePreviousStimulation(Stimulation stimulation) {
        long start = System.currentTimeMillis();
        stimulation.complete();
        long now = System.currentTimeMillis();
        return (now - start) / 1000.0;
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
        synchronized (playing) {
            stop();
            Set<Device> closed = new HashSet<>();
            for (Stimulator stimulator : stimulators.values()) {
                StimulationDevice device = stimulator.getDevice();
                if (!closed.contains(device)) {
                    device.close();
                    closed.add(device);
                }
            }
        }
    }
}
