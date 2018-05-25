package teaselib.stimulation.ext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.ext.StimulationTargets.Samples;

public class StimulationTargtetsTest {
    @Test
    public void testSingleTarget() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleTargetRepeatCount() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5), 0, 2000));
        assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 1.0);
        test(samples.next(), 1500, 0.0);
        test(samples.next(), 2000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleTargetWithOffset() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        int startOffset = 666;
        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5), startOffset));
        assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 0.0);
        test(samples.next(), startOffset, 1.0);
        test(samples.next(), startOffset + 500, 0.0);
        test(samples.next(), startOffset + 1000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testDualTargets() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5)));
        assertEquals(2, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testDualTargets_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 500));
        assertEquals(2, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0);
        test(samples.next(), 1000, 0.0, 0.0);
        test(samples.next(), 1500, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleTargets() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim3, new SquareWave(0.5, 0.5)));
        assertEquals(3, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleTargets_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 500));
        targets.add(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 1000));
        assertEquals(3, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 0.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 1.0);
        test(samples.next(), 1500, 0.0, 0.0, 0.0);
        test(samples.next(), 2000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleTargets_sequential() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        testOrderingAscending(device, stim1, stim2, stim3);
    }

    @Test
    public void testThatTargetOrderIsSameAsStimulatorOrderOfDevice() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));

        testOrderingAscending(device, stim1, stim2, stim3);
        testOrderingDescending(device, stim1, stim2, stim3);
        testOrderingRandom(device, stim1, stim2, stim3);
    }

    private static void testOrderingAscending(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
            Stimulator stim3) {
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.add(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));

        testTripleTargets(targets);
    }

    private static void testOrderingDescending(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
            Stimulator stim3) {
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));
        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));

        testTripleTargets(targets);
    }

    private static void testOrderingRandom(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
            Stimulator stim3) {
        StimulationTargets targets = new StimulationTargets(device);

        targets.add(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.add(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));
        targets.add(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));

        testTripleTargets(targets);
    }

    private static void testTripleTargets(StimulationTargets channels) {
        assertEquals(3, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 0.0, 0.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 1.0, 0.0);
        test(samples.next(), 1500, 0.0, 0.0, 0.0);
        test(samples.next(), 2000, 0.0, 0.0, 1.0);
        test(samples.next(), 2500, 0.0, 0.0, 0.0);
        test(samples.next(), 3000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    private static void test(Samples samples, long expectedTimeStampMillis, double... values) {
        assertEquals(expectedTimeStampMillis, samples.timeStampMillis);
        assertArrayEquals(values, samples.getValues(), 0.0);
    }
}
