package teaselib.stimulation.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        Iterator<Samples> sampleIterator = channels.iterator();
        assertTrue(sampleIterator.hasNext());
        Samples samples = sampleIterator.next();
        assertEquals(1, samples.getValues().length);

        testSample(samples, 0, 0, 1.0);
        testSample(sampleIterator, 500, 0, 0.0);
        testSample(sampleIterator, 1000, 0, 0.0);

        assertFalse(sampleIterator.hasNext());
    }

    @Test
    public void testDualChannels() {
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));
        channels.add(new Channel(null, new SquareWave(0.5, 0.5)));

        assertEquals(2, channels.size());

        Iterator<Samples> sampleIterator = channels.iterator();
        assertTrue(sampleIterator.hasNext());
        Samples samples = sampleIterator.next();
        assertEquals(2, samples.getValues().length);

        testSample(samples, 0, 0, 1.0);
        testSample(samples, 0, 1, 1.0);
        testSample(sampleIterator, 500, 0, 0.0);
        testSample(sampleIterator, 1000, 0, 0.0);

        assertFalse(sampleIterator.hasNext());
    }

    @Test
    public void testSingleChannelWithOffset() {
        int startOffset = 666;
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), startOffset));

        assertEquals(1, channels.size());

        Iterator<Samples> sampleIterator = channels.iterator();
        assertTrue(sampleIterator.hasNext());

        testSample(sampleIterator, 0, 0, 0.0);
        testSample(sampleIterator, startOffset, 0, 1.0);
        testSample(sampleIterator, startOffset + 500, 0, 0.0);
        testSample(sampleIterator, startOffset + 1000, 0, 0.0);

        assertFalse(sampleIterator.hasNext());
    }

    private void testSample(Iterator<Samples> sampleIterator, long timeStamp, int index, double value) {
        Samples samples = sampleIterator.next();
        testSample(samples, timeStamp, index, value);
    }

    private void testSample(Samples samples, long timeStamp, int index, double value) {
        assertEquals(timeStamp, samples.timeStampMillis);
        assertEquals(value, samples.getValues()[index], 0);
    }
}
