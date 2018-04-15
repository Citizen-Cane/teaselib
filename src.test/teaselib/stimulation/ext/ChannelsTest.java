package teaselib.stimulation.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.stimulation.SquareWave;

public class ChannelsTest {
    // TODO Should be 666, because of the start offset, but the offset is hard to handle - replace?
    static final int startOffset = 666;

    @Test
    public void testWaveForm() {
        SquareWave squareWave = new SquareWave(0.5, 0.5);

        assertEquals(1000, squareWave.getDurationMillis());

        assertEquals(0, squareWave.nextTime(-1));
        assertEquals(500, squareWave.nextTime(0));
        assertEquals(1000, squareWave.nextTime(500));
        assertEquals(Long.MAX_VALUE, squareWave.nextTime(1000));

        assertEquals(0.0, squareWave.getValue(-1), 0.0);
        assertEquals(1.0, squareWave.getValue(0), 0.0);
        assertEquals(0.0, squareWave.getValue(500), 0.0);
        assertEquals(0.0, squareWave.getValue(1000), 0.0);

    }

    @Test
    public void testSingleChannel() {
        StimulationChannels channels = new StimulationChannels();
        channels.add(new Channel(null, new SquareWave(0.5, 0.5), startOffset));

        assertEquals(1, channels.size());

        StimulationChannels.SampleIterator sampleIterator = channels.sampleIterator();
        assertTrue(sampleIterator.hasNext());
        assertEquals(0, sampleIterator.timeStampMillis);

        // value @ 0

        // TODO values are 0 index off, e.g. first value should be 0,
        // then 1.0 @ start offset,
        // then 0 @ start offset + 500ms,
        // then still 0 at the end of the waveform

        // value @ startOffset
        double[] values = sampleIterator.next();
        assertEquals(1, values.length);
        assertEquals(startOffset, sampleIterator.timeStampMillis);
        assertEquals(0.0, values[0], 0.0);

        // value @ startOffset + 500
        assertTrue(sampleIterator.hasNext());
        values = sampleIterator.next();
        assertEquals(1, values.length);
        assertEquals(startOffset + 500, sampleIterator.timeStampMillis);
        assertEquals(1.0, values[0], 0.0);

        // value @ startOffset + 1000
        assertTrue(sampleIterator.hasNext());
        values = sampleIterator.next();
        assertEquals(startOffset + 1000, sampleIterator.timeStampMillis);
        assertEquals(0.0, values[0], 0.0);

        assertFalse(sampleIterator.hasNext());
    }
}
