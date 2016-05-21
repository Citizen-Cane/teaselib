package teaselib.core.devices.xinput.stimulation;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

public class XInputStimulationDevice implements StimulationDevice {
    public static final String DeviceClassName = "XInputStimulationDevice";

    private final XInputDevice device;
    private final List<Stimulator> stimulators;

    public XInputStimulationDevice(XInputDevice device) {
        super();
        this.device = device;
        this.stimulators = new ArrayList<Stimulator>(2);
        this.stimulators.addAll(XInputStimulator.getStimulators(this));
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                device.getDevicePath());
    }

    @Override
    public void release() {
    }

    @Override
    public List<Stimulator> stimulators() {
        return stimulators;
    }

    XInputDevice getXInputDevice() {
        return device;
    }

    public boolean isConnected() {
        return device.isConnected();
    }
}
