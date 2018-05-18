package teaselib.stimulation.ext;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.Stimulator;
import teaselib.stimulation.ext.StimulationChannels.Samples;

public class ChannelsTest {
    @Test
    public void testSingleChannel() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        assertEquals(1, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleChannelWithOffset() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        StimulationChannels channels = new StimulationChannels(device);

        int startOffset = 666;
        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5), startOffset));
        assertEquals(1, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 0.0);
        test(samples.next(), startOffset, 1.0);
        test(samples.next(), startOffset + 500, 0.0);
        test(samples.next(), startOffset + 1000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testDualChannels() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5)));
        assertEquals(2, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testDualChannels_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5), 500));
        assertEquals(2, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0);
        test(samples.next(), 1000, 0.0, 0.0);
        test(samples.next(), 1500, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleChannels() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim3, new SquareWave(0.5, 0.5)));
        assertEquals(3, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleChannels_Shifted() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5), 500));
        channels.add(new Channel(stim3, new SquareWave(0.5, 0.5), 1000));
        assertEquals(3, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 0.0, 0.0);
        test(samples.next(), 500, 0.0, 1.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 1.0);
        test(samples.next(), 1500, 0.0, 0.0, 0.0);
        test(samples.next(), 2000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleChannels_sequential() {
        TestStimulationDevice device = new TestStimulationDevice();
        Stimulator stim1 = device.add(new TestStimulator(device, 1));
        Stimulator stim2 = device.add(new TestStimulator(device, 2));
        Stimulator stim3 = device.add(new TestStimulator(device, 3));
        testOrderingAscending(device, stim1, stim2, stim3);
    }

    @Test
    public void testThatChannelOrderIsSameAsStimulatorOrderOfDevice() {
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
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5), 1000));
        channels.add(new Channel(stim3, new SquareWave(0.5, 0.5), 2000));

        testTripleChannels(channels);
    }

    private static void testOrderingDescending(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
            Stimulator stim3) {
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim3, new SquareWave(0.5, 0.5), 2000));
        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5), 1000));
        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));

        testTripleChannels(channels);
    }

    private static void testOrderingRandom(TestStimulationDevice device, Stimulator stim1, Stimulator stim2,
            Stimulator stim3) {
        StimulationChannels channels = new StimulationChannels(device);

        channels.add(new Channel(stim2, new SquareWave(0.5, 0.5), 1000));
        channels.add(new Channel(stim3, new SquareWave(0.5, 0.5), 2000));
        channels.add(new Channel(stim1, new SquareWave(0.5, 0.5)));

        testTripleChannels(channels);
    }

    private static void testTripleChannels(StimulationChannels channels) {
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
