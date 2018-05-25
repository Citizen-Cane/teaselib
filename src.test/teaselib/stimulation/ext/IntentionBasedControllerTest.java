package teaselib.stimulation.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import teaselib.Body;
import teaselib.stimulation.Stimulation;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.pattern.Tease;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

public class IntentionBasedControllerTest {
    private final class TestController extends IntentionBasedController<Intention, Body> {
        Consumer<List<StimulationTarget>> testActionList;
        BiConsumer<StimulationDevice, StimulationTargets> testDeviceEntry;

        public TestController(Consumer<List<StimulationTarget>> testActionList,
                BiConsumer<StimulationDevice, StimulationTargets> testDeviceEntry) {
            this.testActionList = testActionList;
            this.testDeviceEntry = testDeviceEntry;
        }

        @Override
        public void play(List<StimulationTarget> channels) {
            testActionList.accept(channels);
            super.play(channels);
        }

        @Override
        void play(StimulationDevice device, StimulationTargets channels) {
            testDeviceEntry.accept(device, channels);
            super.play(device, channels);
        }

    }

    @Test
    public void testDevicePartitioningJoin() {
        TestStimulationDevice device1 = new TestStimulationDevice();
        TestStimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = device1.add(new TestStimulator(device1, 1));
        Stimulator stim2 = device2.add(new TestStimulator(device2, 2));
        Stimulator stim3 = device2.add(new TestStimulator(device2, 3));

        IntentionBasedController<Intention, Body> c = new TestController(
                (stimulationActions) -> assertEquals(2, stimulationActions.size()), (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1) || (device == device2 && items.size() == 2)));
        c.add(Intention.Pace, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Pain, stim3);

        Stimulation pulse = new Tease();
        Stimulation whip = new Whip();

        c.play(Intention.Tease, pulse, Intention.Pain, whip);
    }

    @Test
    public void testDevicePartitioningSeparate() {
        TestStimulationDevice device1 = new TestStimulationDevice();
        TestStimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = device1.add(new TestStimulator(device1, 1));
        Stimulator stim2 = device2.add(new TestStimulator(device2, 2));
        Stimulator stim3 = device2.add(new TestStimulator(device2, 3));

        IntentionBasedController<Intention, Body> c = new TestController(
                (stimulationActions) -> assertEquals(2, stimulationActions.size()),
                (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1 && items.get(0).stimulator == stim1)
                                || (device == device2 && items.size() == 2 && items.get(0) == StimulationTarget.EMPTY
                                        && items.get(1).stimulator == stim3)));
        c.add(Intention.Pace, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Pain, stim3);

        Stimulation walk = new Walk();
        Stimulation whip = new Whip();
        c.play(Intention.Pace, walk, Intention.Pain, whip);
    }

    @Test
    public void testDevicePartitioningSeparateAndJoined() {
        TestStimulationDevice device1 = new TestStimulationDevice();
        TestStimulationDevice device2 = new TestStimulationDevice();

        Stimulator stim1 = device1.add(new TestStimulator(device1, 1));
        Stimulator stim2 = device2.add(new TestStimulator(device2, 2));
        Stimulator stim3 = device2.add(new TestStimulator(device2, 3));

        IntentionBasedController<Intention, Body> c = new TestController(
                (stimulationActions) -> assertEquals(3, stimulationActions.size()),
                (device, items) -> assertTrue(
                        (device == device1 && items.size() == 1 && items.get(0).stimulator == stim1)
                                || (device == device2 && items.size() == 2 && items.get(0).stimulator == stim2
                                        && items.get(1).stimulator == stim3)));
        c.add(Intention.Pace, stim1);
        c.add(Intention.Tease, stim2);
        c.add(Intention.Pain, stim3);

        Stimulation walk = new Walk();
        Stimulation pulse = new Tease();
        Stimulation whip = new Whip();
        c.play(Intention.Pace, walk, Intention.Tease, pulse, Intention.Pain, whip);
    }
}
