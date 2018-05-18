/**
 * 
 */
package teaselib.stimulation.ext;

import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;
import teaselib.stimulation.Stimulator.ChannelDependency;
import teaselib.stimulation.Stimulator.Output;
import teaselib.stimulation.Stimulator.Signal;

final class TestStimulator implements Stimulator {
    final StimulationDevice device;
    final int id;

    TestStimulator(StimulationDevice device, int id) {
        this.device = device;
        this.id = id;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName() + "_" + id;
    }

    @Override
    public StimulationDevice getDevice() {
        return device;
    }

    @Override
    public ChannelDependency channelDependency() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Output output() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Signal signal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double minimalSignalDuration() {
        return 0.02;
    }

    @Override
    public void play(WaveForm waveform, double durationSeconds, double maxstrength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void extend(double durationSeconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void complete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getName();
    }
}