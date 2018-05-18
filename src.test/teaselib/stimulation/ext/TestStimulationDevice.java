/**
 * 
 */
package teaselib.stimulation.ext;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.BatteryLevel;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;

final class TestStimulationDevice extends StimulationDevice {
    List<Stimulator> stimulators = new ArrayList<>();

    public Stimulator add(Stimulator stimulator) {
        stimulators.add(stimulator);
        return stimulator;
    }

    @Override
    public List<Stimulator> stimulators() {
        return stimulators;
    }

    @Override
    public boolean isWireless() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDevicePath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean connected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BatteryLevel batteryLevel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean active() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void play(StimulationChannels channels, int repeatCount) {
        assertNotNull(channels);
        assertFalse(channels.isEmpty());
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void complete() {
        throw new UnsupportedOperationException();
    }
}