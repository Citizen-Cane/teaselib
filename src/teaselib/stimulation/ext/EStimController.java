package teaselib.stimulation.ext;

import java.util.Iterator;
import java.util.List;

import teaselib.Body;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

public class EStimController extends IntentionBasedController<Intention, Body> {
    public static EStimController init(EStimController eStimController, StimulationDevice device) {
        eStimController.clear();

        List<Stimulator> stimulators = device.stimulators();
        Iterator<Stimulator> stimulator = stimulators.iterator();

        if (stimulators.size() == 3) {
            eStimController.add(Intention.Pace, stimulator.next());
            eStimController.add(Intention.Tease, stimulator.next());
            eStimController.add(Intention.Pain, stimulator.next());
        } else if (stimulators.size() == 2) {
            Stimulator stimulator1 = stimulator.next();
            Stimulator stimulator2 = stimulator.next();
            eStimController.add(Intention.Pace, stimulator1);
            eStimController.add(Intention.Tease, stimulator2);
            eStimController.add(Intention.Pain, stimulator1, stimulator2);
        } else if (stimulators.size() == 1) {
            Stimulator stimulator1 = stimulator.next();
            eStimController.add(Intention.Pace, stimulator1);
            eStimController.add(Intention.Tease, stimulator1);
            eStimController.add(Intention.Pain, stimulator1);
        } else if (stimulators.isEmpty()) {
            // manual device, nothing to setup
        } else {
            throw new IllegalArgumentException("Number of channels not supported: " + device);
        }

        return eStimController;
    }
}
