package teaselib.stimulation;

import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Cum;
import teaselib.stimulation.pattern.Punish;
import teaselib.stimulation.pattern.Run;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Trot;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

public enum StimulationType {
    Walk,
    Trot,
    Run,
    Attention,
    Whip,
    Punish,
    Tease,
    Cum

    ;

    public static StimulationController<StimulationType> dummy() {
        return new StimulationController<StimulationType>();
    }

    public static StimulationController<StimulationType> stimulateAnusAndBalls(Stimulator anus, Stimulator balls) {
        StimulationController<StimulationType> stim = new StimulationController<StimulationType>();
        stim.addRegion(StimulationRegion.Anus, anus);
        stim.addRegion(StimulationRegion.Balls, balls);
        stim.add(StimulationType.Walk, new Walk(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Trot, new Trot(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Run, new Run(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Attention, new Attention(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Whip, new Whip(stim.stimulator(StimulationRegion.Balls)));
        stim.add(StimulationType.Punish, new Punish(stim.stimulator(StimulationRegion.Balls)));
        // There's no teasing involved in this configuration
        return stim;
    }

    public static StimulationController<StimulationType> stimulateCockAndBalls(Stimulator cock, Stimulator balls) {
        StimulationController<StimulationType> stim = new StimulationController<StimulationType>();
        stim.addRegion(StimulationRegion.Cock, cock);
        stim.addRegion(StimulationRegion.Balls, balls);
        stim.add(StimulationType.Walk, new Walk(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Trot, new Trot(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Run, new Run(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Attention, new Attention(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Whip, new Whip(stim.stimulator(StimulationRegion.Balls)));
        stim.add(StimulationType.Punish, new Punish(stim.stimulator(StimulationRegion.Balls)));
        stim.add(StimulationType.Tease, new Tease(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Cum, new Cum(stim.stimulator(StimulationRegion.Cock)));
        return stim;
    }

    public static StimulationController<StimulationType> stimulateCockAndAnus(Stimulator cock, Stimulator anus) {
        StimulationController<StimulationType> stim = new StimulationController<StimulationType>();
        stim.addRegion(StimulationRegion.Cock, cock);
        stim.addRegion(StimulationRegion.Anus, anus);
        stim.add(StimulationType.Walk, new Walk(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Trot, new Trot(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Run, new Run(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Attention, new Attention(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Whip, new Whip(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Punish, new Punish(stim.stimulator(StimulationRegion.Anus)));
        stim.add(StimulationType.Tease, new Tease(stim.stimulator(StimulationRegion.Cock)));
        stim.add(StimulationType.Cum, new Cum(stim.stimulator(StimulationRegion.Cock)));
        return stim;
    }
}
