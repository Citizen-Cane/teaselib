package teaselib.stimulation.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import teaselib.core.devices.BatteryLevel;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.WaveForm;

public class IntentionBasedControllerTest {
    private final class TestController extends IntentionBasedController<Intention> {
        Consumer<List<IntentionBasedController<Intention>.StimulationAction>> testActionList;
        BiConsumer<StimulationDevice, List<IntentionBasedController<Intention>.StimulationAction>> testDeviceEntry;

        public TestController(Consumer<List<IntentionBasedController<Intention>.StimulationAction>> testActionList,
                BiConsumer<StimulationDevice, List<IntentionBasedController<Intention>.StimulationAction>> testDeviceEntry) {
            this.testActionList = testActionList;
            this.testDeviceEntry = testDeviceEntry;
        }

        @Override
        public void play(List<IntentionBasedController<Intention>.StimulationAction> stimulationActions) {
            testActionList.accept(stimulationActions);
            super.play(stimulationActions);
        }

        @Override
        void play(StimulationDevice device, List<IntentionBasedController<Intention>.StimulationAction> items) {
            testDeviceEntry.accept(device, items);
            super.play(device, items);
        }
    }

    private final class TestStimulationDevice extends StimulationDevice {
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
        public List<Stimulator> stimulators() {
            throw new UnsupportedOperationException();
        }
    }

    private final class TestStimulator implements Stimulator {
        final StimulationDevice device;

        TestStimulator(StimulationDevice device) {
            super();
            this.device = device;
        }

        @Override
        public String getDeviceName() {
            return null;
        }

        @Override
        public String getLocation() {
            throw new UnsupportedOperationException();
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
        public double minimalSignalDuration() {
            throw new UnsupportedOperationException();
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
    }

    @Test
    public void testDevicePartitioningJoin() {
        StimulationDevice device1 = new TestStimulationDevice();
        StimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = new TestStimulator(device1);
        Stimulator stim2 = new TestStimulator(device2);
        Stimulator stim3 = new TestStimulator(device2);

        IntentionBasedController<Intention> c = new TestController(
                (stimulationActions) -> assertEquals(2, stimulationActions.size()), (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1) || device == device2 && items.size() == 2));
        c.add(Intention.Rythm, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Punish, stim3);

        c.play(Intention.Tease, null, Intention.Punish, null);
    }

    @Test
    public void testDevicePartitioningSeparate() {
        StimulationDevice device1 = new TestStimulationDevice();
        StimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = new TestStimulator(device1);
        Stimulator stim2 = new TestStimulator(device2);
        Stimulator stim3 = new TestStimulator(device2);

        IntentionBasedController<Intention> c = new TestController(
                (stimulationActions) -> assertEquals(2, stimulationActions.size()),
                (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1) && items.get(0).stimulator == stim1
                                || device == device2 && items.size() == 1 && items.get(0).stimulator == stim3));
        c.add(Intention.Rythm, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Punish, stim3);

        c.play(Intention.Rythm, null, Intention.Punish, null);
    }

    @Test
    public void testDevicePartitioningSeparateAndJoined() {
        StimulationDevice device1 = new TestStimulationDevice();
        StimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = new TestStimulator(device1);
        Stimulator stim2 = new TestStimulator(device2);
        Stimulator stim3 = new TestStimulator(device2);

        IntentionBasedController<Intention> c = new TestController(
                (stimulationActions) -> assertEquals(3, stimulationActions.size()),
                (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1) && items.get(0).stimulator == stim1
                                || device == device2 && items.size() == 2 && items.get(0).stimulator == stim2
                                        && items.get(1).stimulator == stim3));
        c.add(Intention.Rythm, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Punish, stim3);

        c.play(Intention.Rythm, null, Intention.Tease, null, Intention.Punish, null);
    }
}
