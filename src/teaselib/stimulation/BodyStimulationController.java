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
        add(Type.Walk, new Walk(), anus);
        add(Type.Trot, new Trot(), anus);
        add(Type.Run, new Run(), anus);
        add(Type.Attention, new Attention(), anus);
        add(Type.Whip, new Whip(), balls);
        add(Type.Punish, new Punish(), balls);
        // There's no teasing involved in this configuration
    }

    public void stimulateCockAndBalls(Stimulator cock, Stimulator balls) {
        addRegion(StimulationRegion.Cock, cock);
        addRegion(StimulationRegion.Balls, balls);
        add(Type.Walk, new Walk(), cock);
        add(Type.Trot, new Trot(), cock);
        add(Type.Run, new Run(), cock);
        add(Type.Attention, new Attention(), cock);
        add(Type.Whip, new Whip(), balls);
        add(Type.Punish, new Punish(), balls);
        add(Type.Tease, new Tease(), cock);
        add(Type.Cum, new Cum(), cock);
    }

    public void stimulateCockAndAnus(Stimulator cock, Stimulator anus) {
        addRegion(StimulationRegion.Cock, cock);
        addRegion(StimulationRegion.Anus, anus);
        add(Type.Walk, new Walk(), cock);
        add(Type.Trot, new Trot(), cock);
        add(Type.Run, new Run(), cock);
        add(Type.Attention, new Attention(), cock);
        add(Type.Whip, new Whip(), anus);
        add(Type.Punish, new Punish(), anus);
        add(Type.Tease, new Tease(), cock);
        add(Type.Cum, new Cum(), cock);
    }
}
