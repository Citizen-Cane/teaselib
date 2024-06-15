package teaselib.stimulation.ext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.ext.StimulationTargets.Samples;
import teaselib.stimulation.pattern.Attention;
import teaselib.stimulation.pattern.Repeat;
import teaselib.stimulation.pattern.Walk;
import teaselib.stimulation.pattern.Whip;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class StimulationTargtetsTest {
    @Test
    public void testSingleTarget() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        Assertions.assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleTargetRepeatCount() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5), 0, 2000));
        Assertions.assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 1.0);
        test(samples.next(), 1500, 0.0);
        test(samples.next(), 2000, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleTargetRepeatCountTimeStampAndDuration() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5), 0, 2000));
        Assertions.assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 500, 1.0);
        test(samples.next(), 500, 500, 0.0);
        test(samples.next(), 1000, 500, 1.0);
        test(samples.next(), 1500, 500, 0.0);
        test(samples.next(), 2000, Long.MAX_VALUE, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleTargetWithOffset() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationTargets targets = new StimulationTargets(device);

        int startOffset = 666;
        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5), startOffset));
        Assertions.assertEquals(1, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 0.0);
        test(samples.next(), startOffset, 1.0);
        test(samples.next(), startOffset + 500, 0.0);
        test(samples.next(), startOffset + 1000, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testDualTargets() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5)));
        Assertions.assertEquals(2, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testDualTargets_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 500));
        Assertions.assertEquals(2, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0);
        test(samples.next(), 1000, 0.0, 0.0);
        test(samples.next(), 1500, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleTargets() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim3, new SquareWave(0.5, 0.5)));
        Assertions.assertEquals(3, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleTargets_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 500));
        targets.set(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 1000));
        Assertions.assertEquals(3, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 0.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 1.0);
        test(samples.next(), 1500, 0.0, 0.0, 0.0);
        test(samples.next(), 2000, 0.0, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());
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

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.set(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));

        testTripleTargets(targets);
    }

    private static void testOrderingDescending(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
                                               Stimulator stim3) {
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));

        testTripleTargets(targets);
    }

    private static void testOrderingRandom(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
                                           Stimulator stim3) {
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5), 1000));
        targets.set(new StimulationTarget(stim3, new SquareWave(0.5, 0.5), 2000));
        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));

        testTripleTargets(targets);
    }

    private static void testTripleTargets(StimulationTargets channels) {
        Assertions.assertEquals(3, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 0.0, 0.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 1.0, 0.0);
        test(samples.next(), 1500, 0.0, 0.0, 0.0);
        test(samples.next(), 2000, 0.0, 0.0, 1.0);
        test(samples.next(), 2500, 0.0, 0.0, 0.0);
        test(samples.next(), 3000, 0.0, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());
    }

    @Test
    public void testInsertNothing() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationTargets targets = new StimulationTargets(device);

        targets.set(new StimulationTarget(stim1, new SquareWave(0.5, 0.5)));
        targets.set(new StimulationTarget(stim2, new SquareWave(0.5, 0.5)));
        Assertions.assertEquals(2, targets.size());

        Iterator<Samples> samples = targets.iterator();
        test(samples.next(), 0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0);

        Assertions.assertFalse(samples.hasNext());

        StimulationTargets newTargets = new StimulationTargets(device);
        targets.continuedStimulation(newTargets, targets.maxDurationMillis());

        Assertions.assertFalse(samples.hasNext(), "Nothing has been inserted so there shouldn't be any new samples");
    }

    @Test
    public void testSamplingMultipleWaveforms() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        Walk walk = new Walk();
        Attention attention = new Attention();
        Whip whip = new Whip();
        targets.set(new StimulationTarget(stim1, whip.waveform(stim1, 0)));
        targets.set(new StimulationTarget(stim2, attention.waveform(stim2, 0)));
        targets.set(new StimulationTarget(stim3, walk.waveform(stim3, 0)));

        Iterator<Samples> firstStim = targets.iterator();
        while (firstStim.hasNext()) {
            @SuppressWarnings("unused")
            Samples samples = firstStim.next();
        }
    }

    @Test
    public void testSamplingRepeat() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationTargets targets = new StimulationTargets(device);

        Walk step = new Walk();
        Repeat walk = step.over(1, TimeUnit.MINUTES);
        Attention attention = new Attention();
        Whip whip = new Whip();
        targets.set(new StimulationTarget(stim1, whip.waveform(stim1, 0)));
        targets.set(new StimulationTarget(stim2, attention.waveform(stim2, 0)));
        targets.set(new StimulationTarget(stim3, walk.waveform(stim3, 0)));

        Iterator<Samples> firstStim = targets.iterator();
        while (firstStim.hasNext()) {
            @SuppressWarnings("unused")
            Samples samples = firstStim.next();
        }

        StimulationTargets newTargets = new StimulationTargets(device);
        newTargets.set(new StimulationTarget(stim1, whip.waveform(stim1, 0)));
        StimulationTargets continued = targets.continuedStimulation(newTargets,
                targets.maxDurationMillis() - step.waveform(stim1, 0).getDurationMillis());

        Assertions.assertEquals(1000, continued.get(0).getWaveForm().getDurationMillis());
        Assertions.assertEquals(whip.waveform(stim3, 0).getDurationMillis(), continued.get(0).getWaveForm().getDurationMillis());

        Iterator<Samples> secondStim = continued.iterator();
        while (secondStim.hasNext()) {
            @SuppressWarnings("unused")
            Samples samples = secondStim.next();
        }
    }

    // TODO More continuedStimulation() tests

    private static void test(Samples samples, long expectedTimeStampMillis, double... values) {
        Assertions.assertEquals(expectedTimeStampMillis, samples.getTimeStampMillis());
        Assertions.assertArrayEquals(values, samples.getValues(), 0.0);
    }

    private static void test(Samples samples, long expectedTimeStampMillis, long exectedDurationMillis,
                             double... values) {
        Assertions.assertEquals(expectedTimeStampMillis, samples.getTimeStampMillis(), "Time stamp -");
        Assertions.assertEquals(exectedDurationMillis, samples.getDurationMillis(), "Duration -");
        Assertions.assertArrayEquals(values, samples.getValues(), 0.0);
    }
}
