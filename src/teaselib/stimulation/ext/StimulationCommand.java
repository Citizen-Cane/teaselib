package teaselib.stimulation.ext;

import teaselib.stimulation.Stimulation;
import teaselib.stimulation.Stimulator;

public class StimulationCommand {
    public final Stimulator stimulator;
    public final Stimulation stimulation;

    public StimulationCommand(Stimulator stimulator, Stimulation stimulation) {
        this.stimulator = stimulator;
        this.stimulation = stimulation;
    }

    @Override
    public String toString() {
        return stimulator + "=" + stimulation;
    }
}