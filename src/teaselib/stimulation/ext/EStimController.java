package teaselib.stimulation.ext;

import java.util.Iterator;
import java.util.List;

import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

public class EStimController extends IntentionBasedController<Intention> {

    public static EStimController getController(StimulationDevice device) {
        EStimController eStimController = new EStimController();

        List<Stimulator> stimulators = device.stimulators();
        Iterator<Stimulator> stimulator = stimulators.iterator();

        if (stimulators.size() == 3) {
            eStimController.add(Intention.Pace, stimulator.next());
            eStimController.add(Intention.Tease, stimulator.next());
            eStimController.add(Intention.Punish, stimulator.next());
        } else if (stimulators.size() == 2) {
            Stimulator stimulator1 = stimulator.next();
            Stimulator stimulator2 = stimulator.next();
            eStimController.add(Intention.Pace, stimulator1);
            eStimController.add(Intention.Tease, stimulator2);
            eStimController.add(Intention.Punish, stimulator1, stimulator2);
        } else if (stimulators.size() == 1) {
            Stimulator stimulator1 = stimulator.next();
            eStimController.add(Intention.Pace, stimulator1);
            eStimController.add(Intention.Tease, stimulator1);
            eStimController.add(Intention.Punish, stimulator1);
        } else {
            throw new IllegalArgumentException(device + " number of channels not supported");
        }

        return eStimController;
    }
}
