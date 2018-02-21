package teaselib.stimulation;

import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Cum;
import teaselib.stimulation.pattern.Punish;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

public class BodyStimulationController extends StimulationController<BodyStimulationController.Type> {
    public enum Type {
        Walk,
        Trot,
        Run,
        Attention,
        Whip,
        Punish,
        Tease,
        Cum
    }

    public void stimulateAnusAndBalls(Stimulator anus, Stimulator balls) {
        addRegion(StimulationRegion.Anus, anus);
        addRegion(StimulationRegion.Balls, balls);
        add(Type.Walk, new Walk(stimulator(StimulationRegion.Anus)));
        add(Type.Trot, new Trot(stimulator(StimulationRegion.Anus)));
        add(Type.Run, new Run(stimulator(StimulationRegion.Anus)));
        add(Type.Attention, new Attention(stimulator(StimulationRegion.Anus)));
        add(Type.Whip, new Whip(stimulator(StimulationRegion.Balls)));
        add(Type.Punish, new Punish(stimulator(StimulationRegion.Balls)));
        // There's no teasing involved in this configuration
    }

    public void stimulateCockAndBalls(Stimulator cock, Stimulator balls) {
        addRegion(StimulationRegion.Cock, cock);
        addRegion(StimulationRegion.Balls, balls);
        add(Type.Walk, new Walk(stimulator(StimulationRegion.Cock)));
        add(Type.Trot, new Trot(stimulator(StimulationRegion.Cock)));
        add(Type.Run, new Run(stimulator(StimulationRegion.Cock)));
        add(Type.Attention, new Attention(stimulator(StimulationRegion.Cock)));
        add(Type.Whip, new Whip(stimulator(StimulationRegion.Balls)));
        add(Type.Punish, new Punish(stimulator(StimulationRegion.Balls)));
        add(Type.Tease, new Tease(stimulator(StimulationRegion.Cock)));
        add(Type.Cum, new Cum(stimulator(StimulationRegion.Cock)));
    }

    public void stimulateCockAndAnus(Stimulator cock, Stimulator anus) {
        addRegion(StimulationRegion.Cock, cock);
        addRegion(StimulationRegion.Anus, anus);
        add(Type.Walk, new Walk(stimulator(StimulationRegion.Cock)));
        add(Type.Trot, new Trot(stimulator(StimulationRegion.Cock)));
        add(Type.Run, new Run(stimulator(StimulationRegion.Cock)));
        add(Type.Attention, new Attention(stimulator(StimulationRegion.Cock)));
        add(Type.Whip, new Whip(stimulator(StimulationRegion.Anus)));
        add(Type.Punish, new Punish(stimulator(StimulationRegion.Anus)));
        add(Type.Tease, new Tease(stimulator(StimulationRegion.Cock)));
        add(Type.Cum, new Cum(stimulator(StimulationRegion.Cock)));
    }
}
