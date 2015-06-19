/**
 * 
 */
package teaselib.stimulation;

import java.util.HashMap;
import java.util.Map;

import teaselib.stimulation.Stimulation.BodyPart;
import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;

/**
 * @author someone
 *
 *         Bookkeeping for available stimulations on vertain body parts.
 */
public class StimulationController<StimulationType> {
    private final Map<BodyPart, Stimulator> stimulators = new HashMap<BodyPart, Stimulator>();
    private final Map<StimulationType, Stimulation> stimulations = new HashMap<StimulationType, Stimulation>();

    private int intensity;

    public StimulationController() {
        clear();
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
                new Walk(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Punish,
                new Walk(stim.stimulator(Stimulation.BodyPart.Balls)));
        // There's no teasing involved in this configuration
        return stim;
    }

    public static StimulationController<Stimulation.Type> stimulateCockAndBalls(
            Stimulator cock, Stimulator balls) {
        StimulationController<Stimulation.Type> stim = new StimulationController<Stimulation.Type>();
        stim.addRegion(Stimulation.BodyPart.Anus, cock);
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
                new Walk(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Punish,
                new Walk(stim.stimulator(Stimulation.BodyPart.Balls)));
        stim.add(Stimulation.Type.Tease,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Cum,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        return stim;
    }

    public static StimulationController<Stimulation.Type> stimulateCockAndAnus(
            Stimulator cock, Stimulator anus) {
        StimulationController<Stimulation.Type> stim = new StimulationController<Stimulation.Type>();
        stim.addRegion(Stimulation.BodyPart.Anus, cock);
        stim.addRegion(Stimulation.BodyPart.Balls, anus);
        stim.add(Stimulation.Type.Walk,
                new Walk(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Trot,
                new Trot(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Run,
                new Run(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Attention,
                new Attention(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Whip,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Punish,
                new Walk(stim.stimulator(Stimulation.BodyPart.Anus)));
        stim.add(Stimulation.Type.Tease,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
        stim.add(Stimulation.Type.Cum,
                new Walk(stim.stimulator(Stimulation.BodyPart.Cock)));
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

    public Stimulator stimulator(BodyPart region) {
        return stimulators.get(region);
    }

    public void add(StimulationType type, Stimulation stimulation) {
        stimulations.put(type, stimulation);
    }

    public boolean canStimulate(StimulationType type) {
        return stimulations.containsKey(type);
    }

    public void stimulate(StimulationType type, double durationSeconds) {
        if (stimulations.containsKey(type)) {
            stimulations.get(type).play(intensity, durationSeconds);
        }
        throw new IllegalArgumentException("Stimulation type "
                + type.toString() + " not supported");
    }
}
