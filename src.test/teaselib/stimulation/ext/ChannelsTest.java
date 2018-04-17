package teaselib.stimulation.ext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Test;

import teaselib.stimulation.SquareWave;
import teaselib.stimulation.ext.StimulationChannels.Samples;

public class ChannelsTest {
    @Test
    public void testSingleChannel() {
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        assertEquals(1, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0);
        test(samples.next(), 500, 0.0);
        test(samples.next(), 1000, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testSingleChannelWithOffset() {
        int startOffset = 666;
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), startOffset));
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
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        assertEquals(2, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testDualChannels_Shifted() {
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), 500));
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
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        assertEquals(3, channels.size());

        Iterator<Samples> samples = channels.iterator();
        test(samples.next(), 0, 1.0, 1.0, 1.0);
        test(samples.next(), 500, 0.0, 0.0, 0.0);
        test(samples.next(), 1000, 0.0, 0.0, 0.0);

        assertFalse(samples.hasNext());
    }

    @Test
    public void testTripleChannels_Shifted() {
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), 500));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), 1000));
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
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), 1000));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), 2000));
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

    private void test(Samples samples, long expectedTimeStampMillis, double... values) {
        assertEquals(expectedTimeStampMillis, samples.timeStampMillis);
        assertArrayEquals(values, samples.getValues(), 0.0);
    }
}
