/**
 * 
 */
package teaselib.stimulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import teaselib.stimulation.Stimulation.BodyPart;
import teaselib.stimulation.Stimulator.ChannelDependency;
import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Cum;
import teaselib.stimulation.pattern.Punish;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

/**
 * @author someone
 *
 *         Bookkeeping for available stimulations on vertain body parts.
 */
public class StimulationController<StimulationType> {
    /**
     * 
     */
    private static final int SleepTimeForPartiallyDependentStimChannels = 100;
    private final Map<BodyPart, Stimulator> stimulators = new HashMap<BodyPart, Stimulator>();
    private final Map<StimulationType, Stimulation> stimulations = new HashMap<StimulationType, Stimulation>();

    private int intensity;

    private final Map<StimulationType, Stimulation> playing = new HashMap<StimulationType, Stimulation>();

    public StimulationController() {
        clear();
    }

    public static StimulationController<Stimulation.Type> dummy() {
        return new StimulationController<Stimulation.Type>();
    }

    public static StimulationController<Stimulation.Type> stimulateAnusAndBalls(
            Stimulator buttplug, Stimulator balls) {
        StimulationController<Stimulation.Type> stim = new StimulationController<Stimulation.Type>();
        stim.addRegion(Stimulation.BodyPart.Anus, buttplug);
        stim.addRegion(Stimulation.BodyPart.Balls, balls);
        stim.add(Stimulation.Type.Walk,
                new Walk(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Trot,
                new Trot(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Run,
                new Run(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Attention,
                new Attention(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Whip,
                new Whip(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Punish,
                new Punish(stim.stimulator(Stimulation.BodyPart.Balls)));
        // There's no teasing involved in this configuration
        return stim;
    }

    public static StimulationController<Stimulation.Type> stimulateCockAndBalls(
            Stimulator cock, Stimulator balls) {
        StimulationController<Stimulation.Type> stim = new StimulationController<Stimulation.Type>();
        stim.addRegion(Stimulation.BodyPart.Cock, cock);
        stim.addRegion(Stimulation.BodyPart.Balls, balls);
        stim.add(Stimulation.Type.Walk,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Trot,
                new Trot(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Run,
                new Run(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Attention,
                new Attention(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Whip,
                new Whip(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Punish,
                new Punish(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Tease,
                new Tease(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Cum,
                new Cum(stim.stimulator(Stimulation.BodyPart.Cock)));
        return stim;
    }

    public static StimulationController<Stimulation.Type> stimulateCockAndAnus(
            Stimulator cock, Stimulator anus) {
        StimulationController<Stimulation.Type> stim = new StimulationController<Stimulation.Type>();
        stim.addRegion(Stimulation.BodyPart.Cock, cock);
        stim.addRegion(Stimulation.BodyPart.Anus, anus);
        stim.add(Stimulation.Type.Walk,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Trot,
                new Trot(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Run,
                new Run(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Attention,
                new Attention(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Whip,
                new Whip(stim.stimulator(Stimulation.BodyPart.Anus), 0.0));
        stim.add(Stimulation.Type.Punish,
                new Punish(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Tease,
                new Tease(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Cum,
                new Cum(stim.stimulator(Stimulation.BodyPart.Cock)));
        return stim;
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

    public void addRegion(BodyPart region, Stimulator stimulator) {
        stimulators.put(region, stimulator);
    }

    public Set<BodyPart> regions() {
        return stimulators.keySet();
    }

    public Stimulator stimulator(BodyPart region) {
        if (stimulators.containsKey(region)) {
            return stimulators.get(region);
        } else {
            throw new IllegalArgumentException(
                    "No stimulator assigned to body region "
                            + region.toString());
        }
    }

    public void add(StimulationType type, Stimulation stimulation) {
        stimulations.put(type, stimulation);
    }

    public boolean canStimulate(StimulationType type) {
        return stimulations.containsKey(type);
    }

    public boolean canStimulate(BodyPart region) {
        return stimulators.containsKey(region);
    }

    public void stimulate(StimulationType type, double durationSeconds) {
        synchronized (playing) {
            if (stimulations.containsKey(type)) {
                if (playing.containsKey(type)) {
                    final Stimulation stimulation = playing.get(type);
                    stimulation.stop();
                    playing.remove(type);
                }
                final Stimulation stimulation = stimulations.get(type);
                final ChannelDependency channelDependency = stimulation.stimulator
                        .channelDependency();
                if (channelDependency != Stimulator.ChannelDependency.Independent) {
                    for (Stimulation s : playing.values()) {
                        if (s.stimulator.getDevice() == stimulation.stimulator
                                .getDevice()) {
                            if (channelDependency == Stimulator.ChannelDependency.PartiallyDependent) {
                                try {
                                    Thread.sleep(SleepTimeForPartiallyDependentStimChannels);
                                } catch (InterruptedException ignore) {
                                }
                                if (durationSeconds > SleepTimeForPartiallyDependentStimChannels) {
                                    durationSeconds -= SleepTimeForPartiallyDependentStimChannels;
                                }
                                break;
                            } else {
                                // If the channels can't be used simultaneously,
                                // we're going to wait.
                                // Since this changes the script timing it is
                                // advised to trigger the shorter stimulation
                                // first
                                long start = System.currentTimeMillis();
                                s.complete();
                                long now = System.currentTimeMillis();
                                double sleepTimeSeconds = 1.0 / 1000.0 * (now - start);
                                if (durationSeconds > sleepTimeSeconds) {
                                    durationSeconds -= sleepTimeSeconds;
                                }
                                break;
                            }
                        }
                    }
                }
                stimulation.play(intensity, durationSeconds);
                playing.put(type, stimulation);
            } else {
                throw new IllegalArgumentException("Stimulation type "
                        + type.toString() + " not supported");
            }
        }
    }

    public void complete(StimulationType type) {
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
            for (StimulationType type : playing.keySet()) {
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
}
