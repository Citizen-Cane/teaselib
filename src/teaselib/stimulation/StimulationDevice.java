package teaselib.stimulation;

import java.util.List;

import teaselib.core.devices.Device;

public interface StimulationDevice extends Device {
    List<Stimulator> stimulators();
}
